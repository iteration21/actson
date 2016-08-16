// MIT License
//
// Copyright (c) 2016 Michel Kraemer
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package de.undercouch.actson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests {@link JsonParser}
 * @author Michel Kraemer
 */
public class JsonParserTest {
  private static class TestAsyncJsonListener implements JsonEventListener {
    private static enum Type {
      OBJECT, ARRAY
    }

    private String result = "";
    private Deque<Type> types = new ArrayDeque<>();
    private Deque<Integer> elementCounts = new ArrayDeque<>();

    @Override
    public void onStartObject() {
      onValue();
      result += "{";
      elementCounts.push(0);
      types.push(Type.OBJECT);
    }

    @Override
    public void onEndObject() {
      result += "}";
      elementCounts.pop();
      types.pop();
    }

    @Override
    public void onStartArray() {
      onValue();
      result += "[";
      elementCounts.push(0);
      types.push(Type.ARRAY);
    }

    @Override
    public void onEndArray() {
      result += "]";
      elementCounts.pop();
      types.pop();
    }

    @Override
    public void onFieldName(String name) {
      if (elementCounts.peek() > 0) {
        result += ",";
      }
      result += "\"" + name + "\":";
      elementCounts.push(elementCounts.pop() + 1);
    }

    private void onValue() {
      if (types.peek() == Type.ARRAY) {
        if (elementCounts.peek() > 0) {
          result += ",";
        }
        elementCounts.push(elementCounts.pop() + 1);
      }
    }

    @Override
    public void onValue(String value) {
      onValue();
      result += "\"" + value + "\"";
    }

    @Override
    public void onValue(int value) {
      onValue();
      result += value;
    }

    @Override
    public void onValue(double value) {
      onValue();
      result += value;
    }

    @Override
    public void onValue(boolean value) {
      onValue();
      result += value;
    }

    @Override
    public void onValueNull() {
      onValue();
      result += "null";
    }
  }

  /**
   * Parse a JSON string with the {@link JsonParser} and return
   * a new JSON string generated by {@link TestAsyncJsonListener}. Assert
   * that the input JSON string is valid.
   * @param json the JSON string to parse
   * @return the new JSON string
   */
  private String parse(String json) {
    JsonParser parser = new JsonParser();
    TestAsyncJsonListener l = new TestAsyncJsonListener();
    parser.addListener(l);
    for (int j = 0; j < json.length(); ++j) {
      assertTrue(parser.feed(json.charAt(j)));
    }
    assertTrue(parser.done());
    return l.result;
  }

  /**
   * Assert that two JSON objects are equal
   * @param expected the expected JSON object
   * @param actual the actual JSON object
   */
  private static void assertJsonObjectEquals(String expected, String actual) {
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<Map<String, Object>> ref =
        new TypeReference<Map<String, Object>>() { };
    try {
      Map<String, Object> em = mapper.readValue(expected, ref);
      Map<String, Object> am = mapper.readValue(actual, ref);
      assertEquals(em, am);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Assert that two JSON arrays are equal
   * @param expected the expected JSON array
   * @param actual the actual JSON array
   */
  private static void assertJsonArrayEquals(String expected, String actual) {
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<List<Object>> ref = new TypeReference<List<Object>>() { };
    try {
      List<Object> el = mapper.readValue(expected, ref);
      List<Object> al = mapper.readValue(actual, ref);
      assertEquals(el, al);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Test if valid files can be parsed correctly
   * @throws IOException if one of the test files could not be read
   */
  @Test
  public void testPass() throws IOException {
    for (int i = 1; i <= 3; ++i) {
      URL u = getClass().getResource("pass" + i + ".txt");
      String json = IOUtils.toString(u, "UTF-8");
      if (json.startsWith("{")) {
        assertJsonObjectEquals(json, parse(json));
      } else {
        assertJsonArrayEquals(json, parse(json));
      }
    }
  }
  
  /**
   * Test if invalid files cannot be parsed
   * @throws IOException if one of the test files could not be read
   */
  @Test
  public void testFail() throws IOException {
    for (int i = 1; i <= 33; ++i) {
      URL u = getClass().getResource("fail" + i + ".txt");
      String json = IOUtils.toString(u, "UTF-8");
      JsonParser parser;
      if (i == 18) {
        // test for too many nested modes
        parser = new JsonParser(16);
      } else {
        parser = new JsonParser();
      }
      boolean ok = true;
      for (int j = 0; j < json.length(); ++j) {
        ok &= parser.feed(json.charAt(j));
        if (!ok) {
          break;
        }
      }
      if (ok) {
        ok &= parser.done();
      }
      assertFalse(ok);
    }
  }

  /**
   * Test if an empty object is parsed correctly
   */
  @Test
  public void emptyObject() {
    String json = "{}";
    assertJsonObjectEquals(json, parse(json));
  }

  /**
   * Test if an object with one property is parsed correctly
   */
  @Test
  public void simpleObject() {
    String json = "{\"name\": \"Elvis\"}";
    assertJsonObjectEquals(json, parse(json));
  }

  /**
   * Test if an empty array is parsed correctly
   */
  @Test
  public void emptyArray() {
    String json = "[]";
    assertJsonArrayEquals(json, parse(json));
  }

  /**
   * Test if a simple array is parsed correctly
   */
  @Test
  public void simpleArray() {
    String json = "[\"Elvis\", \"Max\"]";
    assertJsonArrayEquals(json, parse(json));
  }

  /**
   * Test if an array with mixed values is parsed correctly
   */
  @Test
  public void mixedArray() {
    String json = "[\"Elvis\", 132, \"Max\", 80.67]";
    assertJsonArrayEquals(json, parse(json));
  }
}
