# "Run if" extension for JUnit

JUnit extension to conditionally run tests based on the results of other tests 

## How to install

Include the following dependency on your pom file:

    <dependency>
	    <groupId>pt.ulusofona.deisi</groupId>
	    <artifactId>junit-extension-run-if</artifactId>
	    <version>0.1.0</version>
	</dependency>
	
## How to use

    @RunWith(ConditionalRunner.class)
    public class TestClass1 {
    
        @Test
        public void test01() {
            ...
        }
    
        @Test
        @RunIf({"test01"})
        public void test02() {
            // this test only runs if test01 passes
        }
    }