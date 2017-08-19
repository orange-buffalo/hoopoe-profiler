package hoopoe.test.core;

public class HoopoePluginProviderImplTest {

//    @Rule
//    public final ExpectedException exceptionExpectation = ExpectedException.none();
//
//    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
//    private HoopoeProfiler profilerMock;
//
//    private HoopoePluginsProvider pluginProvider;
//
//    @Before
//    public void prepareTest() throws Exception {
//        MockitoAnnotations.initMocks(this);
//
//        TestClassLoader classLoader = new TestClassLoader();
//        classLoader.includePackages("hoopoe.core");
//
//        Class providerClass = classLoader.loadClass(HoopoePluginProviderImpl.class.getCanonicalName());
//        pluginProvider = (HoopoePluginsProvider) providerClass.newInstance();
//        pluginProvider.setupProfiler(profilerMock);
//    }
//
//    @Test
//    public void testMissingZip() {
//        when(profilerMock.getConfiguration().getEnabledPlugins())
//                .thenReturn(Collections.singleton("this-plugin-is-no-present"));
//
//        exceptionExpectation.expectMessage("this-plugin-is-no-present.zip is not found.");
//
//        pluginProvider.createPlugins();
//    }
//
//    @Test
//    public void testZipWithoutHoopoeProperties() {
//        when(profilerMock.getConfiguration().getEnabledPlugins())
//                .thenReturn(Collections.singleton("plugin-hoopoe-properties-missing"));
//
//        exceptionExpectation.expectMessage(
//                "plugin-hoopoe-properties-missing.zip does not contain META-INF/hoopoe.properties file.");
//
//        pluginProvider.createPlugins();
//    }
//
//    @Test
//    public void testZipWithoutPluginClassProperty() {
//        when(profilerMock.getConfiguration().getEnabledPlugins())
//                .thenReturn(Collections.singleton("plugin-missing-property"));
//
//        exceptionExpectation.expectMessage(
//                "META-INF/hoopoe.properties in plugin-missing-property.zip does not contain plugin.className property.");
//
//        pluginProvider.createPlugins();
//    }
//
//    @Test
//    public void testZipWithoutClass() {
//        when(profilerMock.getConfiguration().getEnabledPlugins())
//                .thenReturn(Collections.singleton("plugin-class-missing"));
//
//        exceptionExpectation.expectMessage(
//                "plugin-class-missing.zip does not contain class hoopoe.plugins.TestPlugin.");
//
//        pluginProvider.createPlugins();
//    }
//
//    @Test
//    public void testValidPlugin() {
//        when(profilerMock.getConfiguration().getEnabledPlugins())
//                .thenReturn(Collections.singleton("plugin-valid"));
//
//        Collection<HoopoePlugin> actualPlugins = pluginProvider.createPlugins();
//        assertThat(actualPlugins, notNullValue());
//        assertThat(actualPlugins.size(), equalTo(1));
//
//        HoopoePlugin actualPlugin = actualPlugins.iterator().next();
//        assertThat(actualPlugin.getClass().getClassLoader().getClass().getCanonicalName(),
//                equalTo(HoopoeClassLoader.class.getCanonicalName()));
//    }

}
