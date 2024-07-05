

import com.addapay.common.response.ResponseData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.util.Map;


public class JsonMapper {

  private final ObjectMapper objectMapper;

  public JsonMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new JsonException(e);
    }
  }

  public Map<String, Object> toMap(Object object) {
    if (object == null) {
      return Maps.newHashMap();
    }
    return objectMapper.convertValue(object, new TypeReference<>() {
    });
  }

  public <T> T fromJson(String jsonStr, Class<T> type) {
    try {
      return objectMapper.readValue(jsonStr, type);
    } catch (JsonProcessingException e) {
      throw new JsonException(e);
    }
  }

  public <T> T fromJson(String jsonStr, TypeReference<T> type) {
    try {
      return objectMapper.readValue(jsonStr, type);
    } catch (JsonProcessingException e) {
      throw new JsonException(e);
    }
  }

  public <T> T convertValue(Object body, Class<T> type) {
    return objectMapper.convertValue(body, type);
  }

  private static class JsonException extends RuntimeException {

    public JsonException(Throwable cause) {
      super(cause);
    }
  }

  public static void main(String[] args) {
    ResponseData responseData = new ResponseData();
    System.out.println(new JsonMapper(new ObjectMapper()).toMap(responseData));
  }
}
