/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author gilberto
 */
public class Test {
    
    private final Department department = new Department();
    
    @Test
    void test1(){
        department.setName("Teste1");
        assertEquals("Teste1", department.getName());
    }
    
    @Test
    void test2(){
        department.setSite("TesteSite");
        assertEquals("TesteSite", department.getSite());
    }
    
    @Test
    void test3(){
        department.setActive(true);
        assertTrue(department.isActive());
    }    
    
}
