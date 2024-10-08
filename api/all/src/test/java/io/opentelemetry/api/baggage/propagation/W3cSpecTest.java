package io.opentelemetry.api.baggage.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageEntryMetadata;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

// This is a direct translation of
// https://github.com/w3c/baggage/blob/8c215efbeebd3fa4b1aceb937a747e56444f22f3/test/test_baggage.py
// from the Python test suite in the W3C spec repository to Java to validate whether the Java OTel
// SDK implementation of baggage conforms to the W3C spec.

public class W3cSpecTest {

  private static final TextMapGetter<Map<String, String>> getter = new TextMapGetter<Map<String, String>>() {
    @Override
    public Iterable<String> keys(Map<String, String> carrier) {
      return carrier.keySet();
    }

    @Nullable
    @Override
    public String get(Map<String, String> carrier, String key) {
      return carrier.get(key);
    }
  };

  @Test
  void BaggageTest_test_parse_simple() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey=SomeValue"), getter);

    Baggage expectedBaggage = Baggage.builder().put("SomeKey", "SomeValue").build();
    Baggage actualBaggage = Baggage.fromContext(result);
    assertThat(actualBaggage).isEqualTo(expectedBaggage);

    Map<String, String> carrier = new HashMap<>();
    propagator.inject(Context.root().with(actualBaggage), carrier, Map::put);
    assertThat(carrier.get("baggage")).isEqualTo("SomeKey=SomeValue");
  }

  @Test
  void BaggageTest_test_parse_multiple() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(), ImmutableMap.of("baggage",
        "SomeKey=SomeValue;SomeProp,SomeKey2=SomeValue2;ValueProp=PropVal"), getter);

    Baggage expectedBaggage = Baggage.builder()
        .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomeProp"))
        .put("SomeKey2", "SomeValue2", BaggageEntryMetadata.create("ValueProp=PropVal")).build();
    Baggage actualBaggage = Baggage.fromContext(result);
    assertThat(actualBaggage).isEqualTo(expectedBaggage);

    Map<String, String> carrier = new HashMap<>();
    propagator.inject(Context.root().with(actualBaggage), carrier, Map::put);
    // note: since OTel treats metadata as one opaque string, and since it will percent encode the
    // metadata, the equal sign of the original properaty will be percent encoded as well.
    assertThat(carrier.get("baggage")).isEqualTo(
        "SomeKey=SomeValue;SomeProp,SomeKey2=SomeValue2;ValueProp%3DPropVal");
  }

  @Test
  void BaggageTest_test_parse_multiple_ows() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(), ImmutableMap.of("baggage",
            "SomeKey \t = \t SomeValue \t ; \t SomeProp \t , \t SomeKey2 \t = \t SomeValue2 \t ; \t ValueProp \t = \t PropVal"),
        getter);

    // original expectation from W3C baggage spec test suite:
    // Baggage expectedBaggage = Baggage.builder()
    //     .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomeProp"))
    //     .put("SomeKey2", "SomeValue2", BaggageEntryMetadata.create("ValueProp=PropVal"))
    //     .build();

    // modified expectation due to OTel treating metadata as one opaque string:
    Baggage expectedBaggage = Baggage.builder()
        .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomeProp"))
        .put("SomeKey2", "SomeValue2", BaggageEntryMetadata.create("ValueProp \t = \t PropVal"))
        .build();
    Baggage actualBaggage = Baggage.fromContext(result);
    assertThat(actualBaggage).isEqualTo(expectedBaggage);

    Map<String, String> carrier = new HashMap<>();
    propagator.inject(Context.root().with(actualBaggage), carrier, Map::put);
    // note: since OTel treats metadata as one opaque string, and since it will percent encode the
    // metadata, surrounding whitespace and the equal sign of the original properaty will be percent
    // encoded as well.
    assertThat(carrier.get("baggage")).isEqualTo(
        "SomeKey=SomeValue;SomeProp,SomeKey2=SomeValue2;ValueProp%20%09%20%3D%20%09%20PropVal");
  }

  @Test
  void BaggageTest_test_parse_multiple_kv_property() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(), ImmutableMap.of("baggage",
            "SomeKey=SomeValue;SomePropKey=SomePropValue,SomeKey2=SomeValue2;SomePropKey2=SomePropValue2"),
        getter);

    Baggage expectedBaggage = Baggage.builder()
        .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomePropKey=SomePropValue"))
        .put("SomeKey2", "SomeValue2", BaggageEntryMetadata.create("SomePropKey2=SomePropValue2"))
        .build();
    Baggage actualBaggage = Baggage.fromContext(result);
    assertThat(actualBaggage).isEqualTo(expectedBaggage);

    Map<String, String> carrier = new HashMap<>();
    propagator.inject(Context.root().with(actualBaggage), carrier, Map::put);
    // note: since OTel treats metadata as one opaque string, and since it will percent encode the
    // metadata, the equal sign of the original properaty will be percent encoded as well.
    assertThat(carrier.get("baggage")).isEqualTo(
        "SomeKey=SomeValue;SomePropKey%3DSomePropValue,SomeKey2=SomeValue2;SomePropKey2%3DSomePropValue2");
  }

  @Test
  void BaggageTest_test_parse_multiple_kv_property_ows() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(), ImmutableMap.of("baggage",
            "SomeKey \t = \t SomeValue \t ; \t SomePropKey=SomePropValue \t , \t SomeKey2 \t = \t SomeValue2 \t ; \t SomePropKey2 \t = \t SomePropValue2"),
        getter);

    // original expectation from W3C baggage spec test suite:
    // Baggage expectedBaggage = Baggage.builder()
    //    .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomePropKey=SomePropValue"))
    //    .put("SomeKey2", "SomeValue2", BaggageEntryMetadata.create("SomePropKey2=SomePropValue2"))
    //    .build();

    // modified expectation due to OTel treating metadata as one opaque string:
    Baggage expectedBaggage = Baggage.builder()
        .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomePropKey=SomePropValue"))
        .put("SomeKey2", "SomeValue2",
            BaggageEntryMetadata.create("SomePropKey2 \t = \t SomePropValue2")).build();
    Baggage actualBaggage = Baggage.fromContext(result);
    assertThat(actualBaggage).isEqualTo(expectedBaggage);

    Map<String, String> carrier = new HashMap<>();
    propagator.inject(Context.root().with(actualBaggage), carrier, Map::put);
    // note: since OTel treats metadata as one opaque string, and since it will percent encode the
    // metadata, surrounding whitespace and the equal sign of the original properaty will be percent
    // encoded as well.
    assertThat(carrier.get("baggage")).isEqualTo(
        "SomeKey=SomeValue;SomePropKey%3DSomePropValue,SomeKey2=SomeValue2;SomePropKey2%20%09%20%3D%20%09%20SomePropValue2");
  }

  @Test
  void BaggageEntryTest_test_parse_simple() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey=SomeValue"), getter);

    Baggage expectedBaggage = Baggage.builder().put("SomeKey", "SomeValue").build();
    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void BaggageEntryTest_test_parse_multiple_equals() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey=SomeValue=equals"), getter);

    Baggage expectedBaggage = Baggage.builder().put("SomeKey", "SomeValue=equals").build();
    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void BaggageEntryTest_test_parse_percent_encoded() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    // Note: in contrast to the original test in the W3C repository, the string in this test also
    // includes the comma and the backslash (%2C and %5C) to include all printable ASCII characters
    // that MUST be percent encoded.

    String original = "\t \"\';=asdf!@#$%^&*(),\\x";
    String encoded = "%09%20%22%27%3B%3Dasdf%21%40%23%24%25%5E%26%2A%28%29%2C%5Cx";
    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey=" + encoded), getter);

    Baggage expectedBaggage = Baggage.builder().put("SomeKey", original).build();
    Baggage actualBaggage = Baggage.fromContext(result);
    assertThat(actualBaggage).isEqualTo(expectedBaggage);

    Map<String, String> carrier = new HashMap<>();
    propagator.inject(Context.root().with(actualBaggage), carrier, Map::put);
    // the following characters are no longer percent-encoded when being propagated downstream
    // by the Java OTel SDK:
    // '  !  @  $  &  *  (  )
    // all of which are in the baggage-octet range and therefore MAY be percent-encoded (but do not
    // have to be)
    assertThat(carrier.get("baggage")).isEqualTo("SomeKey=%09%20%22'%3B%3Dasdf!@%23$%25%5E&*()%2C%5Cx");
  }

  @Test
  void BaggageEntryTest_test_parse_property() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey=SomeValue;SomeProp"), getter);

    Baggage expectedBaggage = Baggage.builder()
        .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomeProp")).build();
    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void BaggageEntryTest_test_parse_multi_property() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey=SomeValue;SomeProp;SecondProp=PropValue"), getter);

    Baggage expectedBaggage = Baggage.builder()
        .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomeProp;SecondProp=PropValue"))
        .build();
    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void BaggageEntryTest_test_parse_kv_property() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey=SomeValue;SomePropKey=SomePropValue"), getter);

    Baggage expectedBaggage = Baggage.builder()
        .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomePropKey=SomePropValue"))
        .build();
    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void BaggageEntryTest_test_parse_simple_ows() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey \t = \t SomeValue \t "), getter);

    Baggage expectedBaggage = Baggage.builder().put("SomeKey", "SomeValue").build();
    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void BaggageEntryTest_test_parse_percent_encoded_ows() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    String original = "\t \"\';=asdf!@#$%^&*()";
    String encoded = "%09%20%22%27%3B%3Dasdf%21%40%23%24%25%5E%26%2A%28%29";

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey \t = \t " + encoded + " \t "), getter);

    Baggage expectedBaggage = Baggage.builder().put("SomeKey", original).build();
    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void BaggageEntryTest_test_parse_property_ows() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", "SomeKey \t = \t SomeValue \t ; \t SomeProp"), getter);

    Baggage expectedBaggage = Baggage.builder()
        .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomeProp")).build();
    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void BaggageEntryTest_test_parse_multi_property_ows() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(), ImmutableMap.of("baggage",
        "SomeKey \t = \t SomeValue \t ; \t SomeProp \t ; \t SecondProp \t = \t PropValue"), getter);

    // original expectation from W3C baggage spec test suite:
    // Baggage expectedBaggage = Baggage.builder()
    //     .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomeProp;SecondProp=PropValue"))
    //     .build();

    // modified expectation due to OTel treating metadata as one opaque string:
    Baggage expectedBaggage = Baggage.builder().put("SomeKey", "SomeValue",
        BaggageEntryMetadata.create("SomeProp \t ; \t SecondProp \t = \t PropValue")).build();
    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void BaggageEntryTest_test_parse_kv_property_ows() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    Context result = propagator.extract(Context.root(), ImmutableMap.of("baggage",
        "SomeKey \t = \t SomeValue \t ; \t SomePropKey \t = \t SomePropValue"), getter);

    // original expectation from W3C baggage spec test suite:
    // Baggage expectedBaggage = Baggage.builder()
    //   .put("SomeKey", "SomeValue", BaggageEntryMetadata.create("SomePropKey=SomePropValue"))
    //   .build();

    // modified expectation due to OTel treating metadata as one opaque string:
    Baggage expectedBaggage = Baggage.builder().put("SomeKey", "SomeValue",
        BaggageEntryMetadata.create("SomePropKey \t = \t SomePropValue")).build();

    assertThat(Baggage.fromContext(result)).isEqualTo(expectedBaggage);
  }

  @Test
  void LimitsTest_test_serialize_at_least_64() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    StringBuilder value = new StringBuilder();
    for (int i = 0; i < 64; i++) {
      value.append("key");
      value.append(i);
      value.append("=");
      value.append("value");
      if (i < 63) {
        value.append(",");
      }
    }

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", value.toString()), getter);
    Baggage baggage = Baggage.fromContext(result);
    assertThat(baggage.asMap().size()).isEqualTo(64);
  }

  @Test
  void LimitsTest_test_serialize_long_entry() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    // a 1 character
    // = 1 character
    // 0123456789 10 characters * 819 = 8190 characters
    // total 8192 characters
    StringBuilder value = new StringBuilder("a=");
    for (int i = 0; i < 819; i++) {
      value.append("0123456789");
    }

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", value.toString()), getter);
    Baggage baggage = Baggage.fromContext(result);
    // the value does not include "a", so we need to check for 8190 characters in the value
    assertThat(baggage.getEntryValue("a").length()).isEqualTo(8190);

    Map<String, String> carrier = new HashMap<>();
    propagator.inject(Context.root().with(baggage), carrier, Map::put);
    assertThat(carrier.get("baggage").length()).isEqualTo(8192);
  }

  @Test
  void LimitsTest_test_serialize_many_entries() {
    W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();

    // 512 entries with 15 bytes + 1 trailing comma, so 512 * 16 = 8192
    StringBuilder value = new StringBuilder();
    for (int i = 0; i < 512; i++) {
      value.append(String.format(Locale.ROOT, "%03d", i));
      value.append("=");
      value.append("0123456789a");
      if (i < 511) {
        value.append(",");
      } else {
        // we need one characater to make up for the missing comma at the end to get to 8192 character
        value.append("b");
      }
    }

    Context result = propagator.extract(Context.root(),
        ImmutableMap.of("baggage", value.toString()), getter);
    Baggage baggage = Baggage.fromContext(result);
    Map<String, String> carrier = new HashMap<>();
    propagator.inject(Context.root().with(baggage), carrier, Map::put);
    assertThat(carrier.get("baggage").length()).isEqualTo(8192);
  }
}
