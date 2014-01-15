package org.motechproject.mds.builder;

import org.junit.Test;
import org.motechproject.mds.PersistanceClassLoader;
import org.motechproject.mds.ex.EntityBuilderException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EntityBuilderTest {

    @Test
    public void shouldAddDefaultPackageToName() throws Exception {
        String simpleName = "Example";
        String expected = String.format("%s.%s", EntityBuilder.PACKAGE, simpleName);
        EntityBuilder builder = new EntityBuilder().withSimpleName(simpleName);

        assertEquals(expected, builder.getClassName());
    }

    @Test
    public void shouldClearClassBytesWhenDataChanged() throws Exception {
        byte[] expected = new byte[0];

        EntityBuilder builder = new EntityBuilder();
        assertArrayEquals(expected, builder.getClassBytes());

        builder.withSimpleName("Simple");
        assertArrayEquals(expected, builder.getClassBytes());

        builder.withClassLoader(getClass().getClassLoader());
        assertArrayEquals(expected, builder.getClassBytes());

        builder.build();
        builder.withSimpleName("Example");
        assertArrayEquals(expected, builder.getClassBytes());

        builder.build();
        builder.withClassLoader(PersistanceClassLoader.class.getClassLoader());
        assertArrayEquals(expected, builder.getClassBytes());
    }

    @Test
    public void shouldBuildEntity() throws Exception {
        String simpleName = "Test";
        String className = String.format("%s.%s", EntityBuilder.PACKAGE, simpleName);

        EntityBuilder builder = new EntityBuilder()
                .withSimpleName(simpleName)
                .withClassLoader(getClass().getClassLoader());

        builder.build();
        Class<?> clazz = builder.getClassLoader().loadClass(className);

        assertNotNull(clazz);
        assertEquals(className, clazz.getName());
    }

}