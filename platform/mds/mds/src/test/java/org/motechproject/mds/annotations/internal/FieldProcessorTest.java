package org.motechproject.mds.annotations.internal;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.gemini.blueprint.mock.MockBundle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.motechproject.commons.date.model.Time;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.annotations.InSet;
import org.motechproject.mds.annotations.NotInSet;
import org.motechproject.mds.domain.ManyToOneRelationship;
import org.motechproject.mds.domain.OneToManyRelationship;
import org.motechproject.mds.domain.OneToOneRelationship;
import org.motechproject.mds.domain.Type;
import org.motechproject.mds.domain.TypeValidation;
import org.motechproject.mds.dto.EntityDto;
import org.motechproject.mds.dto.FieldDto;
import org.motechproject.mds.dto.MetadataDto;
import org.motechproject.mds.dto.SettingDto;
import org.motechproject.mds.dto.TypeDto;
import org.motechproject.mds.dto.ValidationCriterionDto;
import org.motechproject.mds.reflections.ReflectionsUtil;
import org.motechproject.mds.service.EntityService;
import org.motechproject.mds.service.TypeService;
import org.motechproject.mds.util.Constants;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.reflect.FieldUtils.getDeclaredField;
import static org.apache.commons.lang.reflect.MethodUtils.getAccessibleMethod;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.motechproject.mds.testutil.MemberTestUtil.assertHasField;
import static org.motechproject.mds.testutil.MemberTestUtil.assertHasNoField;

@RunWith(MockitoJUnitRunner.class)
public class FieldProcessorTest {

    @Spy
    private MockBundle bundle = new MockBundle();

    @Mock
    private EntityService entityService;

    @Mock
    private TypeService typeService;

    @Captor
    private ArgumentCaptor<EntityDto> entityCaptor;

    @Captor
    private ArgumentCaptor<FieldDto> fieldCaptor;

    private FieldProcessor processor;

    private EntityDto entity = new EntityDto(1L, Sample.class.getName());

    @Before
    public void setUp() throws Exception {
        processor = new FieldProcessor();
        processor.setTypeService(typeService);
        processor.setEntity(entity);
        processor.setClazz(Sample.class);
        processor.setBundle(bundle);
    }

    @Test
    public void shouldReturnCorrectAnnotation() throws Exception {
        assertEquals(Field.class, processor.getAnnotationType());
    }

    @Test
    public void shouldReturnCorrectElementList() throws Exception {
        List<AnnotatedElement> actual = new ArrayList<>();
        actual.addAll(processor.getElementsToProcess());

        assertEquals(Sample.FIELD_COUNT, actual.size());
        assertHasField(actual, "world");
        assertHasField(actual, "pi");
        assertHasField(actual, "serverDate");
        assertHasField(actual, "localTime");
    }

    @Test
    public void shouldProcessField() throws Exception {
        java.lang.reflect.Field world = getDeclaredField(Sample.class, "world", true);

        doReturn(TypeDto.BOOLEAN).when(typeService).findType(Boolean.class);

        processor.process(world);

        verify(typeService).findType(Boolean.class);

        Collection<FieldDto> fields = processor.getElements();

        assertEquals(1, fields.size());

        FieldDto field = fields.iterator().next();

        assertEquals(entity.getId(), field.getEntityId());
        assertEquals("World", field.getBasic().getDisplayName());
        assertEquals(world.getName(), field.getBasic().getName());
        assertFalse(field.getBasic().isRequired());
        assertEquals("", field.getBasic().getDefaultValue());
        assertEquals("", field.getBasic().getTooltip());

        assertEquals(entity.getId(), field.getEntityId());
        assertEquals(TypeDto.BOOLEAN, field.getType());
    }

    @Test
    public void shouldProcessSetter() throws Exception {
        Method setLocalTime = getAccessibleMethod(Sample.class, "setLocalTime", Time.class);

        doReturn(TypeDto.TIME).when(typeService).findType(Time.class);

        processor.process(setLocalTime);

        verify(typeService).findType(Time.class);

        Collection<FieldDto> fields = processor.getElements();

        assertEquals(1, fields.size());

        FieldDto field = fields.iterator().next();

        assertEquals("Local Time", field.getBasic().getDisplayName());
        assertEquals("localTime", field.getBasic().getName());
        assertTrue(field.getBasic().isRequired());
        assertEquals("", field.getBasic().getDefaultValue());
        assertEquals("", field.getBasic().getTooltip());

        assertEquals(entity.getId(), field.getEntityId());
        assertEquals(TypeDto.TIME, field.getType());
    }

    @Test
    public void shouldProcessGetter() throws Exception {
        Method getServerDate = getAccessibleMethod(Sample.class, "getServerDate", new Class[0]);

        doReturn(TypeDto.DATE).when(typeService).findType(Date.class);

        processor.process(getServerDate);

        verify(typeService).findType(Date.class);

        Collection<FieldDto> fields = processor.getElements();

        assertEquals(1, fields.size());

        FieldDto field = fields.iterator().next();

        assertEquals("Server Date", field.getBasic().getDisplayName());
        assertEquals("serverDate", field.getBasic().getName());
        assertFalse(field.getBasic().isRequired());
        assertEquals("", field.getBasic().getDefaultValue());
        assertEquals("", field.getBasic().getTooltip());

        assertEquals(entity.getId(), field.getEntityId());
        assertEquals(TypeDto.DATE, field.getType());
    }

    @Test
    public void shouldFilterOnlyFieldsOrAccessorWithFieldAnnotation() {
        List<AnnotatedElement> elements = new ArrayList<>();
        elements.addAll(processor.getElementsToProcess());
        assertEquals(Sample.FIELD_COUNT, elements.size());

        boolean filterOnlyMdsFields = true;
        for(AnnotatedElement element : elements) {
            if (!ReflectionsUtil.hasAnnotationSelfOrAccessor(element, Field.class)) {
                filterOnlyMdsFields = false;
                break;
            }
        }
        assertTrue(filterOnlyMdsFields);
        assertHasNoField(elements, "FromOneToManyBi");
        assertHasNoField(elements, "publicWithoutAnnotations");
        assertHasNoField(elements, "notPersistentWithAccessors");
    }

    @Test
    public void shouldAssignFieldValidation() throws Exception {
        Type integer = new Type(Integer.class);
        Type decimal = new Type(Double.class);
        Type string = new Type(String.class);

        TypeValidation intMinValue = new TypeValidation("mds.field.validation.minValue", integer);
        TypeValidation intMaxValue = new TypeValidation("mds.field.validation.maxValue", integer);
        TypeValidation intMustBeInSet = new TypeValidation("mds.field.validation.mustBeInSet", string);
        TypeValidation intCannotBeInSet = new TypeValidation("mds.field.validation.cannotBeInSet", string);

        TypeValidation decMinValue = new TypeValidation("mds.field.validation.minValue", decimal);
        TypeValidation decMaxValue = new TypeValidation("mds.field.validation.maxValue", decimal);
        TypeValidation decMustBeInSet = new TypeValidation("mds.field.validation.mustBeInSet", string);
        TypeValidation decCannotBeInSet = new TypeValidation("mds.field.validation.cannotBeInSet", string);

        TypeValidation regex = new TypeValidation("mds.field.validation.regex", string);
        TypeValidation minLength = new TypeValidation("mds.field.validation.minLength", integer);
        TypeValidation maxLength = new TypeValidation("mds.field.validation.maxLength", integer);

        doReturn(TypeDto.INTEGER).when(typeService).findType(Integer.class);
        doReturn(TypeDto.DOUBLE).when(typeService).findType(Double.class);
        doReturn(TypeDto.STRING).when(typeService).findType(String.class);

        doReturn(integer).when(typeService).getType(intMinValue);
        doReturn(integer).when(typeService).getType(intMaxValue);
        doReturn(string).when(typeService).getType(intMustBeInSet);
        doReturn(string).when(typeService).getType(intCannotBeInSet);

        doReturn(decimal).when(typeService).getType(decMinValue);
        doReturn(decimal).when(typeService).getType(decMaxValue);
        doReturn(string).when(typeService).getType(decMustBeInSet);
        doReturn(string).when(typeService).getType(decCannotBeInSet);

        doReturn(string).when(typeService).getType(regex);
        doReturn(integer).when(typeService).getType(minLength);
        doReturn(integer).when(typeService).getType(maxLength);

        doReturn(asList(intMinValue)).when(typeService).findValidations(TypeDto.INTEGER, DecimalMin.class);
        doReturn(asList(intMaxValue)).when(typeService).findValidations(TypeDto.INTEGER, DecimalMax.class);
        doReturn(asList(intMustBeInSet)).when(typeService).findValidations(TypeDto.INTEGER, InSet.class);
        doReturn(asList(intCannotBeInSet)).when(typeService).findValidations(TypeDto.INTEGER, NotInSet.class);
        doReturn(asList(intMinValue)).when(typeService).findValidations(TypeDto.INTEGER, Min.class);
        doReturn(asList(intMaxValue)).when(typeService).findValidations(TypeDto.INTEGER, Max.class);

        doReturn(asList(decMinValue)).when(typeService).findValidations(TypeDto.DOUBLE, DecimalMin.class);
        doReturn(asList(decMaxValue)).when(typeService).findValidations(TypeDto.DOUBLE, DecimalMax.class);
        doReturn(asList(decMustBeInSet)).when(typeService).findValidations(TypeDto.DOUBLE, InSet.class);
        doReturn(asList(decCannotBeInSet)).when(typeService).findValidations(TypeDto.DOUBLE, NotInSet.class);
        doReturn(asList(decMinValue)).when(typeService).findValidations(TypeDto.DOUBLE, Min.class);
        doReturn(asList(decMaxValue)).when(typeService).findValidations(TypeDto.DOUBLE, Max.class);

        doReturn(asList(regex)).when(typeService).findValidations(TypeDto.STRING, Pattern.class);
        doReturn(asList(minLength, maxLength)).when(typeService).findValidations(TypeDto.STRING, Size.class);
        doReturn(asList(minLength)).when(typeService).findValidations(TypeDto.STRING, DecimalMin.class);
        doReturn(asList(maxLength)).when(typeService).findValidations(TypeDto.STRING, DecimalMax.class);

        processor.execute();
        Collection<FieldDto> fields = processor.getElements();

        FieldDto pi = findFieldWithName(fields, "pi");
        assertCriterion(pi, "mds.field.validation.minValue", "3");
        assertCriterion(pi, "mds.field.validation.maxValue", "4");
        assertCriterion(pi, "mds.field.validation.mustBeInSet", "3,3.14,4");
        assertCriterion(pi, "mds.field.validation.cannotBeInSet", "1,2,5");

        FieldDto epsilon = findFieldWithName(fields, "epsilon");
        assertCriterion(epsilon, "mds.field.validation.minValue", "0.0");
        assertCriterion(epsilon, "mds.field.validation.maxValue", "1.0");
        assertCriterion(epsilon, "mds.field.validation.mustBeInSet", "1,0.75,0.5,0.25,0");
        assertCriterion(epsilon, "mds.field.validation.cannotBeInSet", "-1,2,3");

        FieldDto random = findFieldWithName(fields, "random");
        assertCriterion(random, "mds.field.validation.minValue", "0");
        assertCriterion(random, "mds.field.validation.maxValue", "10");

        FieldDto gaussian = findFieldWithName(fields, "gaussian");
        assertCriterion(gaussian, "mds.field.validation.minValue", "0.0");
        assertCriterion(gaussian, "mds.field.validation.maxValue", "1.0");

        FieldDto poem = findFieldWithName(fields, "poem");
        assertCriterion(poem, "mds.field.validation.regex", "[A-Z][a-z]{9}");
        assertCriterion(poem, "mds.field.validation.minLength", "10");
        assertCriterion(poem, "mds.field.validation.maxLength", "20");

        FieldDto article = findFieldWithName(fields, "article");
        assertCriterion(article, "mds.field.validation.minLength", "100");
        assertCriterion(article, "mds.field.validation.maxLength", "500");
    }

    @Test
    public void shouldReadMaxLengthForStringField() {
        Method getLength400 = getAccessibleMethod(Sample.class, "getLength400", new Class[0]);
        doReturn(TypeDto.STRING).when(typeService).findType(String.class);

        processor.process(getLength400);

        Collection<FieldDto> fields = processor.getElements();
        assertEquals(1, fields.size());
        FieldDto field = fields.iterator().next();

        assertEquals("length400", field.getBasic().getName());
        SettingDto lengthSetting = field.getSetting(Constants.Settings.STRING_MAX_LENGTH);
        assertNotNull(lengthSetting);
        assertEquals(400, lengthSetting.getValue());
    }

    @Test
    public void shouldRecognizeRelationshipTypes() throws NoSuchFieldException {
        when(typeService.findType(OneToOneRelationship.class)).thenReturn(TypeDto.ONE_TO_ONE_RELATIONSHIP);
        when(typeService.findType(OneToManyRelationship.class)).thenReturn(TypeDto.ONE_TO_MANY_RELATIONSHIP);
        when(typeService.findType(ManyToOneRelationship.class)).thenReturn(TypeDto.MANY_TO_ONE_RELATIONSHIP);

        processor.process(Sample.class.getDeclaredField("oneToOneUni"));
        processor.process(Sample.class.getDeclaredField("oneToOneBi"));
        processor.process(Sample.class.getDeclaredField("oneToManyUni"));
        processor.process(Sample.class.getDeclaredField("oneToManyBi"));
        processor.process(RelatedSample.class.getDeclaredField("oneToOneBi2"));
        processor.process(RelatedSample.class.getDeclaredField("manyToOneBi"));

        Collection<FieldDto> fields = processor.getElements();
        assertEquals(6, fields.size());

        assertRelationshipField(findFieldWithName(fields, "oneToOneUni"),
                RelatedSample.class, OneToOneRelationship.class, null);
        assertRelationshipField(findFieldWithName(fields, "oneToOneBi"),
                RelatedSample.class, OneToOneRelationship.class, "oneToOneBi2");
        assertRelationshipField(findFieldWithName(fields, "oneToManyUni"),
                RelatedSample.class, OneToManyRelationship.class, null,
                new ExpectedCascadeSettings(true, false, true), Set.class);
        assertRelationshipField(findFieldWithName(fields, "oneToManyBi"),
                RelatedSample.class, OneToManyRelationship.class, "manyToOneBi",
                new ExpectedCascadeSettings(false, false, true), List.class);
        assertRelationshipField(findFieldWithName(fields, "oneToOneBi2"),
                Sample.class, OneToOneRelationship.class, "oneToOneBi");
        assertRelationshipField(findFieldWithName(fields, "manyToOneBi"),
                Sample.class, ManyToOneRelationship.class, "oneToManyBi");
    }

    private void assertRelationshipField(FieldDto field, Class<?> relatedClass,
                                         Class<?> relationshipType, String relatedFieldName) {
        assertRelationshipField(field, relatedClass, relationshipType, relatedFieldName, null, null);
    }

    private void assertRelationshipField(FieldDto field, Class<?> relatedClass,
                                       Class<?> relationshipType, String relatedFieldName,
                                       ExpectedCascadeSettings expectedCascadeSettings, Class collectionType) {
        assertEquals(relationshipType.getName(), field.getType().getTypeClass());

        MetadataDto md = field.getMetadata(Constants.MetadataKeys.RELATED_CLASS);
        assertNotNull(md);
        assertEquals(relatedClass.getName(), md.getValue());

        if (relatedFieldName != null) {
            md = field.getMetadata(Constants.MetadataKeys.RELATED_FIELD);
            assertNotNull(md);
            assertEquals(relatedFieldName, md.getValue());
        } else {
            assertNull(field.getMetadata(Constants.MetadataKeys.RELATED_FIELD));
        }

        if (expectedCascadeSettings != null) {
            assertEquals(expectedCascadeSettings.isPersist(), getCascadeSetting(field, Constants.Settings.CASCADE_PERSIST));
            assertEquals(expectedCascadeSettings.isUpdate(), getCascadeSetting(field, Constants.Settings.CASCADE_UPDATE));
            assertEquals(expectedCascadeSettings.isDelete(), getCascadeSetting(field, Constants.Settings.CASCADE_DELETE));
        }

        if (collectionType != null) {
            md = field.getMetadata(Constants.MetadataKeys.RELATIONSHIP_COLLECTION_TYPE);
            assertNotNull(md);
            assertEquals(collectionType.getName(), md.getValue());
        }
    }

    private FieldDto findFieldWithName(Collection<FieldDto> fields, String name) {
        return (FieldDto) CollectionUtils.find(
                fields, new BeanPropertyValueEqualsPredicate("basic.name", name)
        );
    }

    private Boolean getCascadeSetting(FieldDto field, String settingStr) {
        SettingDto setting = field.getSetting(settingStr);
        if (setting == null) {
            return null;
        } else {
            return (Boolean) setting.getValue();
        }
    }

    private void assertCriterion(FieldDto field, String displayName, String value) {
        ValidationCriterionDto dto = field.getValidation().getCriterion(displayName);

        assertNotNull("Criterion " + displayName + " should exists", dto);
        assertEquals(value, String.valueOf(dto.getValue()));
        assertTrue("The validation criterion should be enabled", dto.isEnabled());
    }

    private class ExpectedCascadeSettings {

        private final boolean persist;
        private final boolean update;
        private final boolean delete;

        private ExpectedCascadeSettings(boolean persist, boolean update, boolean delete) {
            this.persist = persist;
            this.update = update;
            this.delete = delete;
        }

        public boolean isPersist() {
            return persist;
        }

        public boolean isUpdate() {
            return update;
        }

        public boolean isDelete() {
            return delete;
        }
    }
}
