package com.samaksh.farms.common.exception;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.*;
class DatabaseErrorMessageTest {
 String message(String state,String detail){var ex=new DataIntegrityViolationException("could not execute SQL update sales set updated_by_email=?",new SQLException(detail,state));return new GlobalExceptionHandler().handleDataIntegrityViolation(ex).getBody().getMessage();}
 @Test void salesCheckFailureIsNotReportedAsDuplicateEmail(){String result=message("23514","new row violates check constraint \"sales_quantity_check\"");assertTrue(result.contains("sales_quantity_check"));assertTrue(result.contains("23514"));assertFalse(result.contains("Email already exists"));}
 @Test void actualDuplicateEmailRetainsHelpfulMessage(){assertEquals("Email already exists",message("23505","duplicate key value violates unique constraint \"users_email_key\" Detail: Key (email)=(private@example.test) already exists."));}
 @Test void otherDuplicateAndNotNullErrorsDoNotLeakValues(){String result=message("23505","Key (request_key)=(secret-value) already exists");assertFalse(result.contains("Email"));assertFalse(result.contains("secret-value"));assertTrue(message("23502","null value in column quantity").contains("requires a value"));}
}
