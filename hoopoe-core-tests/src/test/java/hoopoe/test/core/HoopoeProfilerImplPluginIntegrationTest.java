package hoopoe.test.core;

public class HoopoeProfilerImplPluginIntegrationTest {

//    private static final String GUINEAPIGS_PACKAGE = "hoopoe.test.core.guineapigs";
//
//    @Rule
//    public TestConfigurationRule configurationRule = new TestConfigurationRule();
//
//    @Test
//    public void testPluginProviderInitialization() throws Exception {
//        HoopoePluginsProvider pluginsProviderMock = TestConfiguration.getPluginsProviderMock();
//        doReturn(Collections.emptyList())
//                .when(pluginsProviderMock)
//                .createPlugins();
//
//        HoopoeTestExecutor.create()
//                .withContext(testClassLoader -> new Object())
//                .executeWithAgentLoaded(context -> {
//                    // we just test initialization of profiler during agent loading
//                });
//
//        verify(pluginsProviderMock).setupProfiler(any());
//        verify(pluginsProviderMock).createPlugins();
//    }
//
//    @Test
//    public void testPluginRequest() throws Exception {
//        HoopoePlugin pluginMock = preparePluginMock();
//
//        Collection<HoopoeMethodInfoImpl> actualRequests = new HashSet<>();
//        doAnswer(invocation -> {
//            actualRequests.add((HoopoeMethodInfoImpl) invocation.getArguments()[0]);
//            return null;
//        }).when(pluginMock).createRecorderIfSupported(any());
//
//        Class guineaPigClass = PluginGuineaPig.class;
//
//        HoopoeTestExecutor.create()
//                .withPackage(GUINEAPIGS_PACKAGE)
//                .withContext(testClassLoader -> testClassLoader.loadClass(guineaPigClass.getCanonicalName()))
//                .executeWithAgentLoaded(context -> {
//                });
//
//        HashSet<String> superClasses = new HashSet<>(Arrays.asList(
//                Object.class.getCanonicalName(), Serializable.class.getCanonicalName())
//        );
//
//        List<HoopoeMethodInfoImpl> expectedRequests = Arrays.asList(
//                new HoopoeMethodInfoImpl(
//                        guineaPigClass.getCanonicalName(),
//                        "doStuff()",
//                        superClasses),
//                new HoopoeMethodInfoImpl(
//                        guineaPigClass.getCanonicalName(),
//                        "testCache()",
//                        superClasses
//                ),
//                new HoopoeMethodInfoImpl(
//                        guineaPigClass.getCanonicalName(),
//                        "firstMethodInCacheTest()",
//                        superClasses
//                ),
//                new HoopoeMethodInfoImpl(
//                        guineaPigClass.getCanonicalName(),
//                        "secondMethodInCacheTest()",
//                        superClasses
//                ),
//                new HoopoeMethodInfoImpl(
//                        guineaPigClass.getCanonicalName(),
//                        "methodForAttributes(java.lang.Object)",
//                        superClasses
//                ));
//
//        assertThat("Expected request was not triggered", expectedRequests, everyItem(isIn(actualRequests)));
//    }
//
//    @Test
//    public void testUnsupportedPlugin() throws Exception {
//        HoopoePlugin pluginMock = preparePluginMock();
//        when(pluginMock.createRecorderIfSupported(any())).thenReturn(null);
//
//        Class guineaPigClass = PluginGuineaPig.class;
//
//        HoopoeTestExecutor.forClassInstance(guineaPigClass.getCanonicalName())
//                .withPackage(GUINEAPIGS_PACKAGE)
//                .executeWithAgentLoaded(context -> {
//                });
//
//        // basically no verification, just valid execution
//    }
//
//    @Test
//    public void testSupportedPlugin() throws Exception {
//        HoopoePlugin pluginMock = preparePluginMock();
//        HoopoeInvocationRecorder pluginActionMock = Mockito.mock(HoopoeInvocationRecorder.class);
//        when(pluginMock.createRecorderIfSupported(any())).thenReturn(pluginActionMock);
//        when(pluginActionMock.getAttributes(any(), any(), any(), any())).thenReturn(Collections.emptyList());
//
//        TestConfiguration.getIncludeClassesPatterns().add(Pattern.compile(".*PluginAttributesGuineaPig.*"));
//        TestConfiguration.setExcludeClassesPatterns(Collections.singleton(Pattern.compile(".*")));
//
//        Class guineaPigClass = PluginAttributesGuineaPig.class;
//
//        Object argument = new Object();
//        AtomicReference thisInMethod = new AtomicReference();
//
//        HoopoeTestExecutor.forClassInstance(guineaPigClass.getCanonicalName())
//                .withPackage(guineaPigClass.getPackage().getName())
//                .executeWithAgentLoaded(context -> {
//                    Object instance = context.getInstance();
//                    instance.getClass().getMethod("methodForAttributes", Object.class).invoke(instance, argument);
//                    thisInMethod.set(instance);
//                });
//
//        HashSet<String> superClasses = new HashSet<>(Arrays.asList(
//                Object.class.getCanonicalName(), Serializable.class.getCanonicalName())
//        );
//        verify(pluginMock, times(1))
//                .createRecorderIfSupported(
//                        eq(new HoopoeMethodInfoImpl(
//                                guineaPigClass.getCanonicalName(),
//                                "methodForAttributes(java.lang.Object)",
//                                superClasses
//                        )));
//        verify(pluginActionMock, times(1))
//                .getAttributes(
//                        eq(new Object[] {argument}),
//                        eq(null),
//                        eq(thisInMethod.get()),
//                        (HoopoeThreadLocalStorage) argThat(notNullValue()));
//    }
//
//    @Test
//    public void testPluginCache() throws Exception {
//        AtomicInteger pluginCalls = new AtomicInteger();
//        HoopoePlugin plugin = methodInfo -> {
//            if (methodInfo.getMethodSignature().equals("firstMethodInCacheTest()")) {
//                return (arguments, returnValue, thisInMethod, cache) -> {
//                    cache.set(thisInMethod, "testString");
//                    pluginCalls.incrementAndGet();
//                    return Collections.emptyList();
//                };
//            }
//            else if (methodInfo.getMethodSignature().equals("secondMethodInCacheTest()")) {
//                return (arguments, returnValue, thisInMethod, cache) -> {
//                    String actualCachedValue = cache.get(thisInMethod);
//                    assertThat(actualCachedValue, equalTo("testString"));
//                    pluginCalls.incrementAndGet();
//                    return Collections.emptyList();
//                };
//            }
//            return null;
//        };
//
//        HoopoePluginsProvider pluginsProviderMock = TestConfiguration.getPluginsProviderMock();
//        doReturn(Collections.singleton(plugin))
//                .when(pluginsProviderMock)
//                .createPlugins();
//
//        // todo make this class field
//        Class guineaPigClass = PluginGuineaPig.class;
//
//        HoopoeTestExecutor.forClassInstance(guineaPigClass.getCanonicalName())
//                .withPackage(GUINEAPIGS_PACKAGE)
//                .executeWithAgentLoaded(context -> {
//                    Object instance = context.getInstance();
//                    context.getClazz().getMethod("testCache").invoke(instance);
//                });
//
//        assertThat(pluginCalls.get(), equalTo(2));
//    }
//
//    @Test
//    public void testProvidedSuperClassesContainAllInterfacesHierarchy() throws Exception {
//        HoopoePlugin pluginMock = preparePluginMock();
//
//        Class guineaPigClass = ConcreteGuineaPig.class;
//
//        Collection<String> actualSuperclasses = new HashSet<>();
//        doAnswer(invocation -> {
//            HoopoeMethodInfoImpl methodInfo = (HoopoeMethodInfoImpl) invocation.getArguments()[0];
//            if (methodInfo.getCanonicalClassName().equals(guineaPigClass.getCanonicalName())) {
//                actualSuperclasses.addAll(methodInfo.getSuperclasses());
//            }
//            return null;
//        }).when(pluginMock).createRecorderIfSupported(any());
//
//        HoopoeTestExecutor.create()
//                .withPackage("hoopoe.test.core.guineapigs.hierarchy")
//                .withContext(testClassLoader -> testClassLoader.loadClass(guineaPigClass.getCanonicalName()))
//                .executeWithAgentLoaded(context -> {
//                });
//
//        assertThat(
//                actualSuperclasses,
//                containsInAnyOrder(
//                        ISubGuineaPig.class.getCanonicalName(),
//                        ITopGuineaPig.class.getCanonicalName(),
//                        Object.class.getCanonicalName()
//                )
//        );
//    }
//
//    private HoopoePlugin preparePluginMock() {
//        HoopoePlugin pluginMock = Mockito.mock(HoopoePlugin.class);
//
//        HoopoePluginsProvider pluginsProviderMock = TestConfiguration.getPluginsProviderMock();
//        doReturn(Collections.singleton(pluginMock))
//                .when(pluginsProviderMock)
//                .createPlugins();
//
//        return pluginMock;
//    }

}