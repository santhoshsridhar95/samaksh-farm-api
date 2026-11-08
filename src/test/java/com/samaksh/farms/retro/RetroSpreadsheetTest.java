package com.samaksh.farms.retro;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class RetroSpreadsheetTest {
 final RetroSpreadsheet reader=new RetroSpreadsheet();
 byte[] zip(Map<String,String> files)throws Exception{var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){for(var e:files.entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));zip.closeEntry();}}return out.toByteArray();}
 Map<String,String> workbook(String cell){return Map.of("xl/workbook.xml","<workbook xmlns:r='r'><sheets><sheet r:id='rId2'/></sheets></workbook>","xl/_rels/workbook.xml.rels","<Relationships><Relationship Id='rId2' Target='worksheets/sheet2.xml'/></Relationships>","xl/worksheets/sheet2.xml","<worksheet><sheetData><row r='1'><c r='A1' t='inlineStr'><is><t>externalReference</t></is></c><c r='B1' t='inlineStr'><is><t>quantity</t></is></c></row><row r='2'><c r='A2' t='inlineStr'><is><t>old-1</t></is></c>"+cell+"</row></sheetData></worksheet>");}
 @Test void readsExcelFirstSheetByRelationshipAndPreservesValues()throws Exception{var rows=reader.read("history.xlsx",zip(workbook("<c r='B2'><v>12.5</v></c>")));assertEquals("old-1",rows.getFirst().get("externalReference"));assertEquals("12.5",rows.getFirst().get("quantity"));}
 @Test void readsQuotedCsvWithEmbeddedCommaAndNewline(){var rows=reader.read("history.csv","externalReference,notes\nold-1,\"hello, world\nsecond line\"".getBytes(StandardCharsets.UTF_8));assertEquals("hello, world\nsecond line",rows.getFirst().get("notes"));}
 @Test void rejectsFormulasRatherThanUsingStaleCachedResults()throws Exception{assertThrows(IllegalArgumentException.class,()->reader.read("data.xlsx",zip(workbook("<c r='B2'><f>1+1</f><v>2</v></c>"))));}
 @Test void rejectsDuplicateHeadersAndOversizeRowCounts(){assertThrows(IllegalArgumentException.class,()->reader.read("a.csv","id,id\n1,2".getBytes()));String csv="externalReference\n"+"row\n".repeat(201);assertThrows(IllegalArgumentException.class,()->reader.read("a.csv",csv.getBytes()));}
 @Test void parsesExcelSerialAndIsoDatesWithoutChangingBusinessDate(){assertEquals(LocalDate.of(2024,1,1),RetroImportService.convert("45292",LocalDate.class));assertEquals(LocalDate.of(2024,1,1),RetroImportService.convert("2024-01-01",LocalDate.class));assertThrows(IllegalArgumentException.class,()->RetroImportService.convert("01/02/2024",LocalDate.class));}
 @Test void rejectsNonFiniteNumbersAndFractionalIdentifiers(){assertThrows(IllegalArgumentException.class,()->RetroImportService.convert("NaN",Double.class));assertThrows(IllegalArgumentException.class,()->RetroImportService.convert("1.5",Long.class));}
}
