package io.quarkiverse.fx.showcase.pages.web;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import netscape.javascript.JSObject;

/**
 * Java object exposed to JavaScript through {@code JSObject.setMember} : its public methods are invoked reflectively
 * by the WebView JavaScript engine.
 */
@RegisterForReflection
public class WebBridge {

    private final List<String> calls = new ArrayList<>();

    public String greet(String name) {
        calls.add("greet");
        return "Hello " + name + " from Java";
    }

    public int add(int a, int b) {
        calls.add("add");
        return a + b;
    }

    /**
     * Sums a JavaScript array, received as a {@link JSObject}.
     */
    public double sum(JSObject array) {
        calls.add("sum");
        int length = ((Number) array.getMember("length")).intValue();
        double sum = 0;
        for (int i = 0; i < length; i++) {
            sum += ((Number) array.getSlot(i)).doubleValue();
        }
        return sum;
    }

    /**
     * The Java type a JavaScript value was converted to.
     */
    public String describe(Object value) {
        calls.add("describe");
        if (value == null) {
            return "null";
        }
        return value instanceof JSObject ? "JSObject" : value.getClass().getSimpleName();
    }

    public List<String> calls() {
        return List.copyOf(calls);
    }
}
