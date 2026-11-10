package com.samaksh.farms.production.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.enums.BatchStatus;
import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.MushroomType;
import com.samaksh.farms.enums.TransactionType;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import com.samaksh.farms.production.dto.ProductionBatchRequest;
import com.samaksh.farms.production.dto.ProductionBatchResponse;
import com.samaksh.farms.production.dto.RoomTransferRequest;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import com.samaksh.farms.productionmovement.entity.ProductionMovement;
import com.samaksh.farms.productionmovement.repo.ProductionMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@org.springframework.transaction.annotation.Transactional
@Service
@RequiredArgsConstructor
public class ProductionBatchService {

    private final ProductionBatchRepository productionBatchRepository;

    private final InventoryTransactionRepository inventoryTransactionRepository;

    private final ProductionMovementRepository productionMovementRepository;

    private final AuditService auditService;
    private final com.samaksh.farms.employee.WorkAttributionService attribution;
    private final com.samaksh.farms.inventory.InventoryCatalogService catalog;

    public ProductionBatchResponse createBatch(
            ProductionBatchRequest request,
            Authentication authentication
    ) {

        catalog.lock();
        Long creatorId = currentUserId(authentication);
        validateRequest(request);
        attribution.validate(request.getPerformedByEmployeeId(),null,request.getStartDate()==null?com.samaksh.farms.common.time.BusinessTime.today():request.getStartDate(),authentication);
        double calculatedSpawnKg =
                request.getBagsPrepared() * 0.150;

        double calculatedPelletsKg =
                request.getBagsPrepared();

        int calculatedCovers =
                request.getBagsPrepared();

        validateInventory(calculatedSpawnKg + zero(request.getDamagedSpawnKg()), calculatedPelletsKg + zero(request.getDamagedPelletsKg()), calculatedCovers + zero(request.getDamagedCovers()));

        ProductionBatch batch =
                ProductionBatch.builder().performedByEmployeeId(request.getPerformedByEmployeeId())
                        .batchCode(
                                generateBatchCode(
                                        request.getMushroomType()
                                )
                        )
                        .mushroomType(
                                request.getMushroomType()
                        )
                        .bagsPrepared(
                                request.getBagsPrepared()
                        )
                        .damagedCovers(
                                request.getDamagedCovers()
                        )
                        .damagedSpawnKg(
                                request.getDamagedSpawnKg()
                        )
                        .damagedPelletsKg(
                                request.getDamagedPelletsKg()
                        )
                        .spawnUsedKg(
                                calculatedSpawnKg
                        )
                        .pelletsUsedKg(
                                calculatedPelletsKg
                        )
                        .coversUsed(
                                calculatedCovers
                        )
                        .darkRoomBags(
                                request.getBagsPrepared()
                        )
                        .lightRoomBags(
                                0
                        )
                        .contaminatedBags(
                                0
                        )
                        .discardedBags(
                                0
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .startDate(request.getStartDate()==null?com.samaksh.farms.common.time.BusinessTime.today():request.getStartDate())
                        .status(
                                BatchStatus.ACTIVE
                        )
                        .createdBy(creatorId)
                        .createdAt(com.samaksh.farms.common.time.BusinessTime.now())
                        .build();

        ProductionBatch savedBatch =
                productionBatchRepository.save(
                        batch
                );

        createConsumptionTransaction(
                InventoryType.SPAWN,
                calculatedSpawnKg,
                savedBatch.getBatchCode(), savedBatch.getStartDate()
        );

        createConsumptionTransaction(
                InventoryType.PELLET,
                calculatedPelletsKg,
                savedBatch.getBatchCode(), savedBatch.getStartDate()
        );

        createConsumptionTransaction(
                InventoryType.BAG,
                (double) calculatedCovers,
                savedBatch.getBatchCode(), savedBatch.getStartDate()
        );

        createDamageTransaction(
                InventoryType.SPAWN,
                request.getDamagedSpawnKg(),
                savedBatch.getBatchCode(), savedBatch.getStartDate()
        );

        createDamageTransaction(
                InventoryType.PELLET,
                request.getDamagedPelletsKg(),
                savedBatch.getBatchCode(), savedBatch.getStartDate()
        );

        createDamageTransaction(
                InventoryType.BAG,
                request.getDamagedCovers() == null
                        ? 0.0
                        : request.getDamagedCovers().doubleValue(),
                savedBatch.getBatchCode(), savedBatch.getStartDate()
        );

        auditService.createAudit(
                authentication,
                "PRODUCTION",
                "CREATE_BATCH",
                savedBatch.getBatchCode(),
                "Production batch created: " + request
        );

        return mapToResponse(
                savedBatch
        );
    }

    public ProductionBatchResponse transferToLightRoom(
            Long batchId,
            RoomTransferRequest request,
            Authentication authentication
    ) {

        catalog.lock();
        var movementDate=request.getMovementDate()==null?com.samaksh.farms.common.time.BusinessTime.today():request.getMovementDate();
        if(movementDate.isAfter(com.samaksh.farms.common.time.BusinessTime.today()))throw new IllegalArgumentException("Movement date cannot be in the future");
        ProductionBatch batch =
                productionBatchRepository.findById(
                        batchId
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Batch",
                                batchId
                        )
                );

        if(batch.getStartDate()!=null&&movementDate.isBefore(batch.getStartDate()))throw new IllegalArgumentException("Movement cannot precede batch start");
        int moved =
                request.getMovedToLightRoom() == null
                        ? 0
                        : request.getMovedToLightRoom();

        int contaminated =
                request.getContaminatedBags() == null
                        ? 0
                        : request.getContaminatedBags();

        int discarded =
                request.getDiscardedBags() == null
                        ? 0
                        : request.getDiscardedBags();

        int totalProcessed =
                moved
                        + contaminated
                        + discarded;

        if (moved < 0 || contaminated < 0 || discarded < 0 || totalProcessed <= 0) throw new IllegalArgumentException("Enter a positive bag movement");
        if (totalProcessed > batch.getDarkRoomBags()) {

            throw new RuntimeException(
                    "Processed bags exceed available dark room bags"
            );
        }

        attribution.validate(request.getPerformedByEmployeeId(),null,movementDate,authentication);
        batch.setDarkRoomBags(
                batch.getDarkRoomBags()
                        - totalProcessed
        );

        batch.setLightRoomBags(
                batch.getLightRoomBags()
                        + moved
        );

        batch.setContaminatedBags(
                batch.getContaminatedBags()
                        + contaminated
        );

        batch.setDiscardedBags(
                batch.getDiscardedBags()
                        + discarded
        );

        ProductionBatch savedBatch =
                productionBatchRepository.save(
                        batch
                );

        ProductionMovement movement =
                ProductionMovement.builder().performedByEmployeeId(request.getPerformedByEmployeeId())
                        .batch(
                                savedBatch
                        )
                        .movedToLightRoom(
                                moved
                        )
                        .contaminatedBags(
                                contaminated
                        )
                        .discardedBags(
                                discarded
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .movementDate(
                                movementDate.atStartOfDay()
                        )
                        .build();

        productionMovementRepository.save(
                movement
        );

        auditService.createAudit(
                authentication,
                "PRODUCTION",
                "TRANSFER_TO_LIGHT_ROOM",
                savedBatch.getBatchCode(),
                "Movement date " + movementDate + ", " + request
        );

        return mapToResponse(
                savedBatch
        );
    }

    private double zero(Number n) { return n == null ? 0 : n.doubleValue(); }
    private void validateRequest(ProductionBatchRequest r) {
        if(r.getStartDate()!=null&&r.getStartDate().isAfter(com.samaksh.farms.common.time.BusinessTime.today()))throw new IllegalArgumentException("Batch start cannot be in the future");
        if(r.getMushroomType()==null || r.getBagsPrepared()==null || r.getBagsPrepared()<=0) throw new IllegalArgumentException("Choose mushroom type and positive bag count");
        for(double n:new double[]{zero(r.getDamagedCovers()),zero(r.getDamagedSpawnKg()),zero(r.getDamagedPelletsKg())}) if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("Damage cannot be negative");
    }
    public ProductionBatchResponse updateBatch(Long id, ProductionBatchRequest r, Authentication auth) {
        catalog.lock(); validateRequest(r);
        var b=productionBatchRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Batch not found"));
        String before=mapToResponse(b).toString();
        attribution.validate(r.getPerformedByEmployeeId(),b.getPerformedByEmployeeId(),r.getStartDate()==null?b.getStartDate():r.getStartDate(),auth);
        b.setPerformedByEmployeeId(r.getPerformedByEmployeeId());
        int change=r.getBagsPrepared()-b.getBagsPrepared();
        if(b.getDarkRoomBags()+change<0)throw new IllegalArgumentException("Cannot remove bags already transferred or discarded");
        double[] old={zero(b.getSpawnUsedKg())+zero(b.getDamagedSpawnKg()),zero(b.getPelletsUsedKg())+zero(b.getDamagedPelletsKg()),zero(b.getCoversUsed())+zero(b.getDamagedCovers())};
        double[] next={r.getBagsPrepared()*.15+zero(r.getDamagedSpawnKg()),r.getBagsPrepared()+zero(r.getDamagedPelletsKg()),r.getBagsPrepared()+zero(r.getDamagedCovers())};
        InventoryType[] types={InventoryType.SPAWN,InventoryType.PELLET,InventoryType.BAG};
        for(int i=0;i<3;i++) {
            double delta=old[i]-next[i];
            if(getAvailableInventory(types[i])+delta < -0.000001)throw new IllegalArgumentException("Insufficient "+types[i]+" stock");
            if(Math.abs(delta)>0.000001)inventoryTransactionRepository.save(InventoryTransaction.builder().inventoryType(types[i].name()).transactionType("ADJUSTMENT").stockEffect(delta>0?1:-1).quantity(Math.abs(delta)).productionBatchCode(b.getBatchCode()).transactionDate(b.getStartDate()).remarks("Batch edit: "+b.getBatchCode()).createdByEmail(auth.getName()).createdAt(LocalDateTime.now()).build());
        }
        b.setMushroomType(r.getMushroomType());b.setBagsPrepared(r.getBagsPrepared());b.setDarkRoomBags(b.getDarkRoomBags()+change);
        b.setSpawnUsedKg(r.getBagsPrepared()*.15);b.setPelletsUsedKg(r.getBagsPrepared().doubleValue());b.setCoversUsed(r.getBagsPrepared());
        b.setDamagedCovers(r.getDamagedCovers());b.setDamagedSpawnKg(r.getDamagedSpawnKg());b.setDamagedPelletsKg(r.getDamagedPelletsKg());b.setRemarks(r.getRemarks());
        if(r.getStartDate()!=null){if(productionMovementRepository.findAll().stream().anyMatch(m->m.getBatch()!=null&&id.equals(m.getBatch().getId())&&m.getMovementDate()!=null&&m.getMovementDate().toLocalDate().isBefore(r.getStartDate())))throw new IllegalArgumentException("Batch start cannot follow recorded room transfers");b.setStartDate(r.getStartDate());}
        productionBatchRepository.save(b);auditService.createAudit(auth,"PRODUCTION","EDIT_BATCH",b.getBatchCode(),before+" -> "+mapToResponse(b));return mapToResponse(b);
    }
    public List<ProductionBatchResponse> getAllBatches() {

        return productionBatchRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private double getAvailableInventory(
            InventoryType inventoryType
    ) {

        List<InventoryTransaction> transactions =
                inventoryTransactionRepository
                        .findByInventoryType(
                                inventoryType
                        );

        return transactions.stream().mapToDouble(InventoryTransaction::stockDelta).sum();
    }

    private void validateInventory(
            double requiredSpawn,
            double requiredPellets,
            double requiredBags
    ) {

        double availableSpawn =
                getAvailableInventory(
                        InventoryType.SPAWN
                );

        double availablePellets =
                getAvailableInventory(
                        InventoryType.PELLET
                );

        double availableBags =
                getAvailableInventory(
                        InventoryType.BAG
                );

        if (availableSpawn < requiredSpawn) {

            throw new RuntimeException(
                    "Insufficient Spawn Stock. Available : "
                            + availableSpawn
                            + " KG, Required : "
                            + requiredSpawn
                            + " KG"
            );
        }

        if (availablePellets < requiredPellets) {

            throw new RuntimeException(
                    "Insufficient Pellet Stock. Available : "
                            + availablePellets
                            + " KG, Required : "
                            + requiredPellets
                            + " KG"
            );
        }

        if (availableBags < requiredBags) {

            throw new RuntimeException(
                    "Insufficient Bag Stock. Available : "
                            + availableBags
                            + ", Required : "
                            + requiredBags
            );
        }
    }

    private ProductionBatchResponse mapToResponse(
            ProductionBatch batch
    ) {

        return ProductionBatchResponse.builder().performedByEmployeeId(batch.getPerformedByEmployeeId())
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .mushroomType(batch.getMushroomType())
                .spawnUsed(batch.getSpawnUsedKg())
                .pelletsUsed(batch.getPelletsUsedKg())
                .bagsUsed(
                        batch.getBagsPrepared() == null
                                ? 0.0
                                : batch.getBagsPrepared().doubleValue()
                )
                .darkRoomBags(batch.getDarkRoomBags())
                .lightRoomBags(batch.getLightRoomBags())
                .contaminatedBags(batch.getContaminatedBags())
                .discardedBags(batch.getDiscardedBags())
                .damagedCovers(batch.getDamagedCovers()).damagedSpawnKg(batch.getDamagedSpawnKg()).damagedPelletsKg(batch.getDamagedPelletsKg()).remarks(batch.getRemarks()).startDate(batch.getStartDate())
                .status(batch.getStatus())
                .build();
    }

    private void createConsumptionTransaction(
            InventoryType inventoryType,
            Double quantity,
            String batchCode, LocalDate date
    ) {

        if (quantity == null || quantity <= 0) {
            return;
        }

        inventoryTransactionRepository.save(
                InventoryTransaction.builder()
                        .inventoryType(inventoryType.name())
                        .transactionType(TransactionType.CONSUMPTION.name()).stockEffect(-1).productionBatchCode(batchCode).transactionDate(date)
                        .quantity(quantity)
                        .remarks("Consumed in batch " + batchCode)
                        .createdByUserId(0L)
                        .createdByEmail("SYSTEM")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private void createDamageTransaction(
            InventoryType inventoryType,
            Double quantity,
            String batchCode, LocalDate date
    ) {

        if (quantity == null || quantity <= 0) {
            return;
        }

        inventoryTransactionRepository.save(
                InventoryTransaction.builder()
                        .inventoryType(inventoryType.name())
                        .transactionType(TransactionType.DAMAGE.name()).stockEffect(-1).productionBatchCode(batchCode).transactionDate(date)
                        .quantity(quantity)
                        .remarks("Damaged during batch " + batchCode)
                        .createdByUserId(0L)
                        .createdByEmail("SYSTEM")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private String generateBatchCode(
            MushroomType type
    ) {

        String prefix =
                switch (type) {

                    case OYSTER -> "SF-OYS";

                    case BUTTON -> "SF-BTN";

                    case MILKY -> "SF-MLK";
                };

        String date =
                LocalDate.now()
                        .toString()
                        .replace("-", "");

        long count =
                productionBatchRepository.count() + 1;

        return prefix +
                "-"
                + date
                + "-"
                + String.format("%03d", count);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.samaksh.farms.user.entity.User user) {
            return user.getId();
        }
        return null;
    }
}