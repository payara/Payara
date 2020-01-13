package fish.payara.samples.datagridencryption.websession;

@RunWith(Arquillian.class)
public class WebsessionEncryptionTest {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "sfsb-passivation.war")
                .addPackage("fish.payara.samples.datagridencryption.sfsb");
    }

    @BeforeClass
    public static void enableSecurity() {
        ServerOperations.enableDataGridEncryption();
    }

    @AfterClass
    public static void resetSecurity() {
        ServerOperations.disableDataGridEncryption();
    }


}