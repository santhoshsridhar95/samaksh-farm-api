package com.samaksh.farms.config;
import com.samaksh.farms.auth.jwt.*;
import com.samaksh.farms.user.repo.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
class RoleAccessTest {
 AnnotationConfigWebApplicationContext context;MockMvc mvc;
 @Configuration @EnableWebMvc @EnableWebSecurity @Import(SecurityConfig.class)
 static class Setup {
  @Bean ObjectMapper mapper(){return new ObjectMapper();}
  @Bean JwtAuthenticationFilter jwt(){return new JwtAuthenticationFilter(mock(JwtService.class),mock(UserRepository.class),mapper());}
  @Bean Probe probe(){return new Probe();}
 }
 @RestController static class Probe {
  @RequestMapping({"/api/money","/api/sourcing","/api/orders","/api/farm-dashboard","/api/customers","/api/customers/1","/api/sales","/api/direct-sales","/api/production","/api/inventory-transactions","/api/harvest"}) String ok(){return "ok";}
 }
 @BeforeEach void setup(){context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Setup.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @AfterEach void close(){context.close();}
 @Test void salesEmployeesCanSellButCannotChangeShopSetupOrAccessOtherModules()throws Exception{
  for(String role:new String[]{"SALES_EMPLOYEE","SALES_USER"}){
   for(String path:new String[]{"/api/sales","/api/direct-sales"})mvc.perform(post(path).with(user("staff").roles(role))).andExpect(status().isOk());
   mvc.perform(get("/api/customers").with(user("staff").roles(role))).andExpect(status().isOk());
   mvc.perform(post("/api/customers").with(user("staff").roles(role))).andExpect(status().isForbidden());
   mvc.perform(put("/api/customers/1").with(user("staff").roles(role))).andExpect(status().isForbidden());
   for(String path:new String[]{"/api/money","/api/sourcing","/api/orders","/api/production","/api/farm-dashboard"})mvc.perform(get(path).with(user("staff").roles(role))).andExpect(status().isForbidden());
  }
 }
 @Test void salesAdminCanManageShopsAndFarmRolesOnlyFarmWorkflows()throws Exception{
  mvc.perform(post("/api/customers").with(user("salesadmin").roles("SALES_ADMIN"))).andExpect(status().isOk());
  for(String role:new String[]{"FARM_MANAGER","LABOUR"}){
   for(String path:new String[]{"/api/production","/api/harvest","/api/inventory-transactions"})mvc.perform(get(path).with(user("farm").roles(role))).andExpect(status().isOk());
   for(String path:new String[]{"/api/sales","/api/direct-sales","/api/money","/api/sourcing","/api/orders","/api/farm-dashboard"})mvc.perform(get(path).with(user("farm").roles(role))).andExpect(status().isForbidden());
  }
 }
 @Test void adminRetainsAccessAndAnonymousIsRejected()throws Exception{
  for(String path:new String[]{"/api/money","/api/sourcing","/api/orders","/api/sales","/api/production"})mvc.perform(get(path).with(user("admin").roles("SUPER_ADMIN"))).andExpect(status().isOk());
  mvc.perform(get("/api/money")).andExpect(status().isUnauthorized());
 }
}
