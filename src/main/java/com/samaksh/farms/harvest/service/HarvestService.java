package com.samaksh.farms.harvest.service;
import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.harvest.dto.*;
import com.samaksh.farms.harvest.entity.Harvest;
import com.samaksh.farms.harvest.repo.HarvestRepository;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
public class HarvestService {
    private final HarvestRepository harvestRepository;
    private final ProductionBatchRepository batchRepository;
    private final AuditService auditService;
    private final com.samaksh.farms.employee.WorkAttributionService attribution;
    public HarvestResponse createHarvest(HarvestRequest r, Authentication auth) { return save(null,r,auth); }
    public HarvestResponse updateHarvest(Long id, HarvestRequest r, Authentication auth) { return save(id,r,auth); }
    private HarvestResponse save(Long id, HarvestRequest r, Authentication auth) {
        if(r.getQuantity()==null || !Double.isFinite(r.getQuantity()) || r.getQuantity()<=0) throw new IllegalArgumentException("Harvest quantity must be greater than zero");
        var date=r.getHarvestDate()==null?BusinessTime.today():r.getHarvestDate();
        if(date.isAfter(BusinessTime.today()))throw new IllegalArgumentException("Harvest date cannot be in the future");
        var batch=r.getBatchId()==null?null:batchRepository.findById(r.getBatchId()).orElseThrow(()->new ResourceNotFoundException("Batch",r.getBatchId()));
        var h=id==null?new Harvest():harvestRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Harvest",id));
        String before=id==null?"":mapToResponse(h).toString();
        attribution.validate(r.getPerformedByEmployeeId(), h.getPerformedByEmployeeId(), date, auth);
        Integer boxes=null;
        if(r.getNumberOfBoxes()!=null){try{boxes=r.getNumberOfBoxes().intValueExact();}catch(ArithmeticException ex){throw new IllegalArgumentException("Number of boxes must be a whole number within range");}if(boxes<0)throw new IllegalArgumentException("Number of boxes cannot be negative");}
        h.setPerformedByEmployeeId(r.getPerformedByEmployeeId());h.setNumberOfBoxes(boxes);
        h.setBatch(batch);h.setHarvestDate(date);h.setQuantity(r.getQuantity());h.setRemarks(r.getRemarks());
        // Set creator and timestamp for new entries
        if (id == null) {
            h.setCreatedBy(currentUserId(auth));
            h.setCreatedAt(BusinessTime.now());
        }
        var saved=harvestRepository.save(h);
        auditService.createAudit(auth,"HARVEST",id==null?"CREATE_HARVEST":"EDIT_HARVEST",String.valueOf(saved.getId()),before+" -> "+mapToResponse(saved));
        return mapToResponse(saved);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.samaksh.farms.user.entity.User user) {
            return user.getId();
        }
        return null;
    }
    public List<HarvestResponse> getAllHarvests(){return harvestRepository.findAll().stream().map(this::mapToResponse).toList();}
    private HarvestResponse mapToResponse(Harvest h){return HarvestResponse.builder().id(h.getId()).performedByEmployeeId(h.getPerformedByEmployeeId()).numberOfBoxes(h.getNumberOfBoxes()).batchId(h.getBatch()==null?null:h.getBatch().getId()).batchCode(h.getBatch()==null?null:h.getBatch().getBatchCode()).harvestDate(h.getHarvestDate()).quantity(h.getQuantity()).remarks(h.getRemarks()).createdBy(h.getCreatedBy()).createdAt(h.getCreatedAt()).build();}
}
