package org.hl7.cql.model;

import org.hl7.elm_modelinfo.r1.*;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.xml.bind.JAXB;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.util.*;
import java.util.stream.Collectors;

public class ModelInfoComparer {

    @Test
    public void compareModelInfo() {
        ModelInfo a = JAXB.unmarshal(ModelInfoComparer.class.getResourceAsStream("a-modelinfo.xml"), ModelInfo.class);
        ModelInfo b = JAXB.unmarshal(ModelInfoComparer.class.getResourceAsStream("b-modelinfo.xml"), ModelInfo.class);

        ModelInfoCompareContext differences = new ModelInfoCompareContext();
        compareModelInfo(differences, a, b);
        assertThat(differences.toString(), is(String.format("ModelInfo.Type info allowedUnits in left only%n" +
                "ModelInfo.Conversion from FHIR.Count to System.Quantity in left only%n" +
                "ModelInfo.Conversion from FHIR.Age to System.Quantity in left only%n" +
                "ModelInfo.Conversion from FHIR.MoneyQuantity to System.Quantity in left only%n" +
                "ModelInfo.Conversion from FHIR.Distance to System.Quantity in left only%n" +
                "ModelInfo.Conversion from FHIR.Duration to System.Quantity in left only%n" +
                "ModelInfo.Conversion from FHIR.id to System.String in left only%n" +
                "ModelInfo.Conversion from FHIR.SimpleQuantity to System.Quantity in left only%n")));
        //assertThat(differences.length(), is(0));
    }

    public class ModelInfoCompareContext {
        private StringBuilder differences = new StringBuilder();
        private List<String> focusList = new ArrayList<String>();

        public ModelInfoCompareContext() {
            pushFocus("ModelInfo");
        }

        public String getFocus() {
            return String.join(".", focusList);
        }

        public void pushFocus(String newFocus) {
            focusList.add(newFocus);
        }

        public void popFocus() {
            if (focusList.size() > 0) {
                focusList.remove(focusList.size() - 1);
            }
        }

        public void append(String message) {
            differences.append(getFocus());
            differences.append(".");
            differences.append(message);
            differences.append(String.format("%n"));
        }

        public int length() {
            return differences.length();
        }

        @Override
        public String toString() {
            return differences.toString();
        }
    }

    public static void compareModelInfo(ModelInfoCompareContext context, ModelInfo a, ModelInfo b) {

        compareAttribute(context, "url", a.getUrl(), b.getUrl());
        compareAttribute(context, "version", a.getVersion(), b.getVersion());
        compareAttribute(context, "targetUrl", a.getTargetUrl(), b.getTargetUrl());
        compareAttribute(context, "targetVersion", a.getTargetVersion(), b.getTargetVersion());
        compareAttribute(context, "defaultContext", a.getDefaultContext(), b.getDefaultContext());
        compareAttribute(context, "patientClassName", a.getPatientClassName(), b.getPatientClassName());
        compareAttribute(context, "patientClassIdentifier", a.getPatientClassIdentifier(), b.getPatientClassIdentifier());
        compareAttribute(context, "patientBirthDatePropertyName", a.getPatientBirthDatePropertyName(), b.getPatientBirthDatePropertyName());

        // requiredModelInfo
        Map<String, ModelSpecifier> msa = a.getRequiredModelInfo().stream().collect(Collectors.toMap(k -> k.getName(), v -> v));
        Map<String, ModelSpecifier> msb = b.getRequiredModelInfo().stream().collect(Collectors.toMap(k -> k.getName(), v -> v));

        for (Map.Entry<String, ModelSpecifier> ms : msa.entrySet()) {
            ModelSpecifier msOther = msb.getOrDefault(ms.getKey(), null);
            compareModelSpecifier(context, ms.getValue(), msOther);
        }

        for (Map.Entry<String, ModelSpecifier> ms : msb.entrySet()) {
            ModelSpecifier msOther = msa.getOrDefault(ms.getKey(), null);
            if (msOther == null) {
                compareModelSpecifier(context, msOther, ms.getValue());
            }
        }

        // typeInfo
        Map<String, TypeInfo> tia = a.getTypeInfo().stream().filter(x -> x instanceof ClassInfo || x instanceof SimpleTypeInfo).collect(Collectors.toMap(k -> k instanceof ClassInfo ? ((ClassInfo)k).getName() : ((SimpleTypeInfo)k).getName(), v -> v));
        Map<String, TypeInfo> tib = b.getTypeInfo().stream().filter(x -> x instanceof ClassInfo || x instanceof SimpleTypeInfo).collect(Collectors.toMap(k -> k instanceof ClassInfo ? ((ClassInfo)k).getName() : ((SimpleTypeInfo)k).getName(), v -> v));

        for (Map.Entry<String, TypeInfo> ti : tia.entrySet()) {
            TypeInfo tiOther = tib.getOrDefault(ti.getKey(), null);
            compareTypeInfo(context, ti.getValue(), tiOther);
        }

        for (Map.Entry<String, TypeInfo> ti : tib.entrySet()) {
            TypeInfo tiOther = tia.getOrDefault(ti.getKey(), null);
            if (tiOther == null) {
                compareTypeInfo(context, tiOther, ti.getValue());
            }
        }

        // conversionInfo
        Map<String, ConversionInfo> cia = a.getConversionInfo().stream().collect(Collectors.toMap(k -> k.getFromType(), v -> v));
        Map<String, ConversionInfo> cib = b.getConversionInfo().stream().collect(Collectors.toMap(k -> k.getFromType(), v -> v));

        for (Map.Entry<String, ConversionInfo> ci : cia.entrySet()) {
            ConversionInfo ciOther = cib.getOrDefault(ci.getKey(), null);
            compareConversionInfo(context, ci.getValue(), ciOther);
        }

        for (Map.Entry<String, ConversionInfo> ci : cib.entrySet()) {
            ConversionInfo ciOther = cia.getOrDefault(ci.getKey(), null);
            if (ciOther == null) {
                compareConversionInfo(context, ciOther, ci.getValue());
            }
        }

        // contextInfo
        Map<String, ContextInfo> cxa = a.getContextInfo().stream().collect(Collectors.toMap(k -> k.getName(), v -> v));
        Map<String, ContextInfo> cxb = b.getContextInfo().stream().collect(Collectors.toMap(k -> k.getName(), v -> v));

        for (Map.Entry<String, ContextInfo> ci : cxa.entrySet()) {
            ContextInfo ciOther = cxb.getOrDefault(ci.getKey(), null);
            compareContextInfo(context, ci.getValue(), ciOther);
        }

        for (Map.Entry<String, ContextInfo> ci : cxb.entrySet()) {
            ContextInfo ciOther = cxa.getOrDefault(ci.getKey(), null);
            if (ciOther == null) {
                compareContextInfo(context, ciOther, ci.getValue());
            }
        }
    }

    public static void compareAttribute(ModelInfoCompareContext context, String attributeName, String a, String b) {
        if (a == null || b == null || !a.equals(b)) {
            if (a == null && b == null) {
                return;
            }
            context.append(String.format("%s: %s <> %s", attributeName, a, b));
        }
    }

    public static void compareAttribute(ModelInfoCompareContext context, String attributeName, Boolean a, Boolean b) {
        if (a == null || b == null || !a.equals(b)) {
            if (a == null && b == null) {
                return;
            }
            context.append(String.format("%s: %s <> %s", attributeName, a, b));
        }
    }

    public static void compareModelSpecifier(ModelInfoCompareContext context, ModelSpecifier a, ModelSpecifier b) {
        if (a == null) {
            context.append(String.format("Model specifier %s|%s in right only", b.getName(), b.getVersion()));
        }
        else if (b == null) {
            context.append(String.format("Model specifier %s|%s in left only", a.getName(), b.getVersion()));
        }
        else {
            compareAttribute(context, "version", a.getVersion(), b.getVersion());
        }
    }

    public static String descriptor(NamedTypeSpecifier namedTypeSpecifier) {
        if (namedTypeSpecifier != null) {
            if (namedTypeSpecifier.getNamespace() != null) {
                return String.format("%s.%s", namedTypeSpecifier.getNamespace(), namedTypeSpecifier.getName());
            }

            if (namedTypeSpecifier.getModelName() != null) {
                return String.format("%s.%s", namedTypeSpecifier.getModelName(), namedTypeSpecifier.getName());
            }

            return namedTypeSpecifier.getName();
        }

        return null;
    }

    public static String descriptor(IntervalTypeSpecifier intervalTypeSpecifier) {
        if (intervalTypeSpecifier != null) {
            return String.format("Interval<%s>", descriptor(intervalTypeSpecifier.getPointType(), intervalTypeSpecifier.getPointTypeSpecifier()));
        }

        return null;
    }

    public static String descriptor(ListTypeSpecifier listTypeSpecifier) {
        if (listTypeSpecifier != null) {
            return String.format("List<%s>", descriptor(listTypeSpecifier.getElementType(), listTypeSpecifier.getElementTypeSpecifier()));
        }

        return null;
    }

    public static String descriptor(TupleTypeSpecifier tupleTypeSpecifier) {
        if (tupleTypeSpecifier != null) {
            // TODO: Expand this...
            return "Tuple<...>";
        }

        return null;
    }

    public static String descriptor(ChoiceTypeSpecifier choiceTypeSpecifier) {
        if (choiceTypeSpecifier != null) {
            // TODO: Expand this
            return "Choice<...>";
        }

        return null;
    }

    public static String descriptor(TypeSpecifier typeSpecifier) {
        if (typeSpecifier instanceof NamedTypeSpecifier) {
            return descriptor((NamedTypeSpecifier)typeSpecifier);
        }

        if (typeSpecifier instanceof IntervalTypeSpecifier) {
            return descriptor((IntervalTypeSpecifier)typeSpecifier);
        }

        if (typeSpecifier instanceof ListTypeSpecifier) {
            return descriptor((ListTypeSpecifier)typeSpecifier);
        }

        if (typeSpecifier instanceof TupleTypeSpecifier) {
            return descriptor((TupleTypeSpecifier)typeSpecifier);
        }

        if (typeSpecifier instanceof ChoiceTypeSpecifier) {
            return descriptor((ChoiceTypeSpecifier)typeSpecifier);
        }

        return null;
    }

    public static String descriptor(String elementType, TypeSpecifier elementTypeSpecifier) {
        if (elementType != null) {
            return elementType;
        }

        return descriptor(elementTypeSpecifier);
    }

    public static String descriptor(ListTypeInfo listTypeInfo) {
        if (listTypeInfo != null) {
            return String.format("List<%s>", descriptor(listTypeInfo.getElementType(), listTypeInfo.getElementTypeSpecifier()));
        }

        return null;
    }

    public static String descriptor(IntervalTypeInfo intervalTypeInfo) {
        if (intervalTypeInfo != null) {
            return String.format("Interval<%s>", descriptor(intervalTypeInfo.getPointType(), intervalTypeInfo.getPointTypeSpecifier()));
        }

        return null;
    }

    public static String descriptor(TupleTypeInfo tupleTypeInfo) {
        if (tupleTypeInfo != null) {
            // TODO: Expand this
            return "Tuple<...>";
        }

        return null;
    }

    public static String descriptor(ChoiceTypeInfo choiceTypeInfo) {
        if (choiceTypeInfo != null) {
            // TODO: Expand this
            return "Choice<...>";
        }

        return null;
    }

    public static String descriptor(TypeInfo typeInfo) {
        if (typeInfo instanceof ClassInfo) {
            return ((ClassInfo)typeInfo).getName();
        }
        if (typeInfo instanceof SimpleTypeInfo) {
            return ((SimpleTypeInfo)typeInfo).getName();
        }
        if (typeInfo instanceof ListTypeInfo) {
            return descriptor((ListTypeInfo)typeInfo);
        }
        if (typeInfo instanceof IntervalTypeInfo) {
            return descriptor((IntervalTypeInfo)typeInfo);
        }
        if (typeInfo instanceof TupleTypeInfo) {
            return descriptor((TupleTypeInfo)typeInfo);
        }
        if (typeInfo instanceof ChoiceTypeInfo) {
            return descriptor((ChoiceTypeInfo)typeInfo);
        }

        return null;
    }

    public static void compareTypeInfo(ModelInfoCompareContext context, SimpleTypeInfo a, SimpleTypeInfo b) {
        String descriptorA = descriptor(a);
        String descriptorB = descriptor(b);
        if (descriptorA == null || descriptorB == null || !descriptorA.equals(descriptorB)) {
            context.append(String.format("%s <> %s", descriptorA, descriptorB));
        }
    }

    public static void compareTypeInfo(ModelInfoCompareContext context, ClassInfo a, ClassInfo b) {
        if (a == null || b == null) {
            context.append(String.format("%s <> %s", descriptor(a), descriptor(b)));
        }

        context.pushFocus(a.getName());
        try {
            compareAttribute(context, "baseType", descriptor(a.getBaseType(), a.getBaseTypeSpecifier()), descriptor(b.getBaseType(), b.getBaseTypeSpecifier()));
            compareAttribute(context, "label", a.getLabel(), b.getLabel());
            compareAttribute(context, "identifier", a.getIdentifier(), b.getIdentifier());
            compareAttribute(context, "primaryCodePath", a.getPrimaryCodePath(), b.getPrimaryCodePath());
            compareAttribute(context, "primaryValueSetPath", a.getPrimaryValueSetPath(), b.getPrimaryValueSetPath());
            compareAttribute(context, "target", a.getTarget(), b.getTarget());

            for (int i = 0; i < Math.max(a.getElement().size(), b.getElement().size()); i++) {
                if (i >= a.getElement().size()) {
                    context.append(String.format("Element %s in right only", b.getElement().get(i).getName()));
                }
                else if (i >= b.getElement().size()) {
                    context.append(String.format("Element %s in left only", a.getElement().get(i).getName()));
                }
                else {
                    compareClassInfoElement(context, a.getElement().get(i), b.getElement().get(i));
                }
            }
        }
        finally {
            context.popFocus();
        }
    }

    public static void compareClassInfoElement(ModelInfoCompareContext context, ClassInfoElement a, ClassInfoElement b) {
        context.pushFocus(a.getName());
        try {
            compareAttribute(context, "name", a.getName(), b.getName());
            compareAttribute(context, "type", descriptor(a.getElementType(), a.getElementTypeSpecifier()), descriptor(b.getElementType(), b.getElementTypeSpecifier()));
            compareAttribute(context, "target", a.getTarget(), b.getTarget());
            compareAttribute(context, "isOneBased", a.isOneBased(), b.isOneBased());
            compareAttribute(context, "isProhibited", a.isProhibited(), b.isProhibited());
        }
        finally {
            context.popFocus();
        }
    }

    public static void compareTypeInfo(ModelInfoCompareContext context, IntervalTypeInfo a, IntervalTypeInfo b) {
        String descriptorA = descriptor(a);
        String descriptorB = descriptor(b);
        if (descriptorA == null || descriptorB == null || !descriptorA.equals(descriptorB)) {
            context.append(String.format("%s <> %s", descriptorA, descriptorB));
        }
    }

    public static void compareTypeInfo(ModelInfoCompareContext context, ListTypeInfo a, ListTypeInfo b) {
        String descriptorA = descriptor(a);
        String descriptorB = descriptor(b);
        if (descriptorA == null || descriptorB == null || !descriptorA.equals(descriptorB)) {
            context.append(String.format("%s <> %s", descriptorA, descriptorB));
        }
    }

    public static void compareTypeInfo(ModelInfoCompareContext context, TupleTypeInfo a, TupleTypeInfo b) {
        String descriptorA = descriptor(a);
        String descriptorB = descriptor(b);
        if (descriptorA == null || descriptorB == null || !descriptorA.equals(descriptorB)) {
            context.append(String.format("%s <> %s", descriptorA, descriptorB));
        }
    }

    public static void compareTypeInfo(ModelInfoCompareContext context, ChoiceTypeInfo a, ChoiceTypeInfo b) {
        String descriptorA = descriptor(a);
        String descriptorB = descriptor(b);
        if (descriptorA == null || descriptorB == null || !descriptorA.equals(descriptorB)) {
            context.append(String.format("%s <> %s", descriptorA, descriptorB));
        }
    }

    public static void compareTypeInfo(ModelInfoCompareContext context, TypeInfo a, TypeInfo b) {
        if (a == null) {
            context.append(String.format("Type info %s in right only", descriptor(b)));
        }
        else if (b == null) {
            context.append(String.format("Type info %s in left only", descriptor(a)));
        }
        else if (!a.getClass().equals(b.getClass())) {
            context.append(String.format("Type info %s is %s in left, but %s in right", descriptor(a), a.getClass().getSimpleName(), descriptor(b)));
        }
        else if (a instanceof SimpleTypeInfo) {
            compareTypeInfo(context, (SimpleTypeInfo)a, (SimpleTypeInfo)b);
        }
        else if (a instanceof ClassInfo) {
            compareTypeInfo(context, (ClassInfo)a, (ClassInfo)b);
        }
        else if (a instanceof IntervalTypeInfo) {
            compareTypeInfo(context, (IntervalTypeInfo)a, (IntervalTypeInfo)b);
        }
        else if (a instanceof ListTypeInfo) {
            compareTypeInfo(context, (ListTypeInfo)a, (ListTypeInfo)b);
        }
        else if (a instanceof ChoiceTypeInfo) {
            compareTypeInfo(context, (ChoiceTypeInfo)a, (ChoiceTypeInfo)b);
        }
        else if (a instanceof TupleTypeInfo) {
            compareTypeInfo(context, (TupleTypeInfo)a, (TupleTypeInfo)b);
        }
    }

    public static String descriptor(ConversionInfo conversionInfo) {
        if (conversionInfo != null) {
            return String.format("Conversion from %s to %s",
                    descriptor(conversionInfo.getFromType(), conversionInfo.getFromTypeSpecifier()),
                    descriptor(conversionInfo.getToType(), conversionInfo.getToTypeSpecifier()));
        }

        return null;
    }

    public static void compareConversionInfo(ModelInfoCompareContext context, ConversionInfo a, ConversionInfo b) {
        if (a == null) {
            context.append(String.format("%s in right only", descriptor(b)));
        }
        else if (b == null) {
            context.append(String.format("%s in left only", descriptor(a)));
        }
        else {
            String descriptorA = descriptor(a);
            String descriptorB = descriptor(b);
            if (!descriptorA.equals(descriptorB)) {
                context.append(String.format("%s <> %s", descriptorA, descriptorB));
            }
            compareAttribute(context, "functionName", a.getFunctionName(), b.getFunctionName());
        }
    }

    public static String descriptor(ContextInfo contextInfo) {
        if (contextInfo != null) {
            return String.format("Context %s", contextInfo.getName());
        }

        return null;
    }

    public static void compareContextInfo(ModelInfoCompareContext context, ContextInfo a, ContextInfo b) {
        if (a == null) {
            context.append(String.format("%s in right only", descriptor(b)));
        }
        else if (b == null) {
            context.append(String.format("%s in left only", descriptor(a)));
        }
        else {
            compareAttribute(context, "contextType", descriptor(a.getContextType()), descriptor(b.getContextType()));
            compareAttribute(context, "keyElement", a.getKeyElement(), b.getKeyElement());
            compareAttribute(context, "birthDateElement", a.getBirthDateElement(), b.getBirthDateElement());
        }
    }
}
