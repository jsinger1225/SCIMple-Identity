/**
 * 
 */
package edu.psu.swe.scim.spec.resources;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.psu.swe.scim.spec.annotation.ScimAttribute;
import edu.psu.swe.scim.spec.phonenumber.PhoneNumberLexer;
import edu.psu.swe.scim.spec.phonenumber.PhoneNumberParseException;
import edu.psu.swe.scim.spec.phonenumber.PhoneNumberParseTreeListener;
import edu.psu.swe.scim.spec.phonenumber.PhoneNumberParser;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * Scim core schema, <a
 * href="https://tools.ietf.org/html/rfc7643#section-4.1.2>section 4.1.2</a>
 *
 */

@XmlType
@XmlAccessorType(XmlAccessType.NONE)
@Data
public class PhoneNumber extends KeyedResource implements Serializable {

  private static final long serialVersionUID = 607319505715224096L;
  
  private static final String VISUAL_SEPARATORS = "[\\(\\)\\-\\.]";

  @Setter(AccessLevel.NONE)
  @XmlElement
  @ScimAttribute(description = "Phone number of the User")
  String value;

  @XmlElement
  @ScimAttribute(description = "A human readable name, primarily used for display purposes. READ-ONLY.")
  String display;

  @XmlElement
  @ScimAttribute(canonicalValueList = { "work", "home", "mobile", "fax", "pager", "other" }, description = "A label indicating the attribute's function; e.g., 'work' or 'home' or 'mobile' etc.")
  String type;

  @XmlElement
  @ScimAttribute(description = "A Boolean value indicating the 'primary' or preferred attribute value for this attribute, e.g. the preferred phone number or primary phone number. The primary attribute value 'true' MUST appear no more than once.")
  Boolean primary;

  String rawValue;
  boolean isGlobalNumber = false;
  String number;
  String extension;
  String subAddress;
  String phoneContext;
  boolean isDomainPhoneContext = false;
  Map<String, String> params;
  
  public void addParam(String name, String value) {
    if (this.params == null) {
      this.params = new HashMap<String, String>();
    }

    this.params.put(name, value);
  }

  public void setValue(String value) throws PhoneNumberParseException {
    PhoneNumberLexer phoneNumberLexer = new PhoneNumberLexer(new ANTLRInputStream(value));

    List<? extends Token> allTokens = phoneNumberLexer.getAllTokens();
    allTokens.stream()
             .forEach(System.out::println);

    phoneNumberLexer = new PhoneNumberLexer(new ANTLRInputStream(value));

    PhoneNumberParser p = new PhoneNumberParser(new CommonTokenStream(phoneNumberLexer));
    p.setBuildParseTree(true);

    p.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
      }
    });

    PhoneNumberParseTreeListener tpl = new PhoneNumberParseTreeListener();
    try {
      ParseTree tree = p.phoneNumber();
      ParseTreeWalker.DEFAULT.walk(tpl, tree);
    } catch (IllegalStateException e) {
      throw new PhoneNumberParseException(e);
    }
      
    PhoneNumber parsedPhoneNumber = tpl.getPhoneNumber();
    
    this.value = value;
    this.rawValue = value;
    this.number = parsedPhoneNumber.getNumber();
    this.extension = parsedPhoneNumber.getExtension();
    this.subAddress = parsedPhoneNumber.getSubAddress();
    this.phoneContext = parsedPhoneNumber.getPhoneContext();
    this.params = parsedPhoneNumber.getParams();
    this.isGlobalNumber = parsedPhoneNumber.isGlobalNumber();
    this.isDomainPhoneContext = parsedPhoneNumber.isDomainPhoneContext();
  }
  
  /*
   * Implements RFC 3996 URI Equality for the value property
   * https://tools.ietf.org/html/rfc3966#section-3
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PhoneNumber other = (PhoneNumber) obj;
    
    if (isGlobalNumber != other.isGlobalNumber)
      return false;
    
    
    String numberWithoutVisualSeparators = number != null ? number.replaceAll(VISUAL_SEPARATORS, "") : null;
    String otherNumberWithoutVisualSeparators = other.number != null ? other.number.replaceAll(VISUAL_SEPARATORS, "") : null;
    if (numberWithoutVisualSeparators == null) {
      if (otherNumberWithoutVisualSeparators != null)
        return false;
    } else if (!numberWithoutVisualSeparators.equals(otherNumberWithoutVisualSeparators))
      return false;
    
    
    String extensionWithoutVisualSeparators = extension != null ? extension.replaceAll(VISUAL_SEPARATORS, "") : null;
    String otherExtensionWithoutVisualSeparators = other.extension != null ? other.extension.replaceAll(VISUAL_SEPARATORS, "") : null;
    if (extensionWithoutVisualSeparators == null) {
      if (otherExtensionWithoutVisualSeparators != null)
        return false;
    } else if (!extensionWithoutVisualSeparators.equals(otherExtensionWithoutVisualSeparators))
      return false;
    
    
    if (subAddress == null) {
      if (other.subAddress != null)
        return false;
    } else if (!subAddress.equalsIgnoreCase(other.subAddress))
      return false;

    
    String phoneContextTemp = phoneContext;
    if (!StringUtils.isBlank(phoneContext) && !isDomainPhoneContext) {
      phoneContextTemp = phoneContext.replaceAll(VISUAL_SEPARATORS, "");
    }
    
    String otherPhoneContextTemp = other.phoneContext;
    if (!StringUtils.isBlank(other.phoneContext) && !other.isDomainPhoneContext) {
      otherPhoneContextTemp = other.phoneContext.replaceAll(VISUAL_SEPARATORS, "");
    }

    if (phoneContextTemp == null) {
      if (otherPhoneContextTemp != null)
        return false;
    } else if (!phoneContextTemp.equalsIgnoreCase(otherPhoneContextTemp))
      return false;
    
    
    if (!equalsIgnoreCaseAndOrderParams(other.params)) {
      return false;
    }
        
    
    if (primary == null) {
      if (other.primary != null)
        return false;
    } else if (!primary.equals(other.primary))
      return false;
    
    
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equalsIgnoreCase(other.type))
      return false;
    
    
    return true;
  }

  /*
   * Implements RFC 3996 URI Equality for the value property
   * https://tools.ietf.org/html/rfc3966#section-3
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isGlobalNumber ? 1231 : 1237);
    result = prime * result + ((number == null) ? 0 : number.replaceAll(VISUAL_SEPARATORS, "").hashCode());
    result = prime * result + ((extension == null) ? 0 : extension.replaceAll(VISUAL_SEPARATORS, "").hashCode());
    result = prime * result + ((subAddress == null) ? 0 : subAddress.toLowerCase().hashCode());
    result = prime * result + ((phoneContext == null) ? 0 : (isDomainPhoneContext ? phoneContext.toLowerCase().hashCode() : phoneContext.replaceAll(VISUAL_SEPARATORS, "").hashCode()));
    result = prime * result + ((params == null) ? 0 : paramsToLowerCase().hashCode());
    result = prime * result + ((primary == null) ? 0 : primary.hashCode());
    result = prime * result + ((type == null) ? 0 : type.toLowerCase().hashCode());
    return result;
  }
  
  HashMap<String, String> paramsToLowerCase() {
    HashMap<String, String> paramsLowercase = new HashMap<String, String>();
    for(Entry<String, String> entry : params.entrySet()) {
      paramsLowercase.put(entry.getKey().toLowerCase(), entry.getValue().toLowerCase());
    }
    
    return paramsLowercase;
  }
  
  boolean equalsIgnoreCaseAndOrderParams(Map<String, String> otherParams) {
    if (params == null && otherParams == null) {
      return true;
    }
    
    if ((params == null && otherParams != null) ||
        (params != null && otherParams == null) ||
        (params.size() != otherParams.size())) {
      return false;
    }
    
    HashMap<String, String> paramsLowercase = paramsToLowerCase(); 
    
    for(Entry<String, String> entry : otherParams.entrySet()) {
      String foundValue = paramsLowercase.get(entry.getKey().toLowerCase());
      
      if (!entry.getValue().equalsIgnoreCase(foundValue)) {
        return false;
      }
    }
    
    return true;
  }
  
  protected static class PhoneNumberBuilder {
    
    static final Logger LOGGER = LoggerFactory.getLogger(PhoneNumberBuilder.class);

    final String HYPHEN = "-";
    final String INTERNATIONAL_PREFIX = "+";
    final String PREFIX = "tel:%s";
    final String EXTENSTION_PREFIX = ";ext=%s";
    final String ISUB_PREFIX = ";isub=%s";
    final String CONTEXT_PREFIX = ";phone-context=%s";
    final String PARAMS_STRING = ";%s=%s";
    final String LOCAL_SUBSCRIBER_NUMBER_REGEX = "^[\\d\\.\\-\\(\\)]+$";
    final String DOMAIN_NAME_REGEX = "^[a-zA-Z0-9\\.\\-]+$";
    final String GLOBAL_NUMBER_REGEX = "^(\\+)?[\\d\\.\\-\\(\\)]+$";
    final String COUNTRY_CODE_REGEX = "^(\\+)?[1-9][0-9]{0,2}$";
    
    String number;
    String display;
    String extension;
    String subAddress;
    String phoneContext;
    Map<String, String> params;

    void setParam(String name, String value) {
      if (this.params == null) {
        this.params = new HashMap<String, String>();
      }

      this.params.put(name, value);
    }

    String getFormattedExtension() {
      if (this.extension != null && !this.extension.isEmpty()) {
        return String.format(EXTENSTION_PREFIX, this.extension);
      }

      return null;
    }

    String getFormattedSubAddress() {
      if (this.subAddress != null && !this.subAddress.isEmpty()) {
        return String.format(ISUB_PREFIX, this.subAddress);
      }

      return null;
    }

    String getFormattedPhoneContext() {
      if (this.phoneContext != null && !this.phoneContext.isEmpty()) {
        return String.format(CONTEXT_PREFIX, this.phoneContext);
      }

      return null;
    }

    String getFormattedParams() {
      String paramsFormatted = "";

      if (params != null) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
          paramsFormatted += String.format(PARAMS_STRING, entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
      }
      
      return !paramsFormatted.isEmpty() ? paramsFormatted : null;
    }

    String getFormattedValue() {
      String valueString = String.format(PREFIX, this.number);

      String fExtension = getFormattedExtension();
      if (fExtension != null) {
        valueString += fExtension;
      }

      String fSubAddr = getFormattedSubAddress();
      if (fSubAddr != null) {
        valueString += fSubAddr;
      }

      String fContext = getFormattedPhoneContext();
      if (fContext != null) {
        valueString += fContext;
      }

      String fParams = getFormattedParams();
      if (fParams != null) {
        valueString += fParams;
      }

      return !valueString.isEmpty() ? valueString : null;
    }

    PhoneNumber build() throws PhoneNumberParseException {
      if (!StringUtils.isBlank(extension) && !StringUtils.isBlank(subAddress)) {
        throw new IllegalArgumentException("PhoneNumberBuilder cannot have a value for both extension and subAddress.");
      }
      
      if (extension != null && !extension.matches(LOCAL_SUBSCRIBER_NUMBER_REGEX)) {
        throw new IllegalArgumentException("PhoneNumberBuilder extension must contain only numeric characters and optional ., -, (, ) visual separator characters.");
      }
      
      if (params != null && !params.isEmpty()) {
        if (params.get("") != null ||
            params.get(null) != null ||
            params.values().contains(null) ||
            params.values().contains("")) {
          throw new IllegalArgumentException("PhoneNumberBuilder params names and values cannot be null or empty."); 
        }
      }
      
      PhoneNumber phoneNumber = new PhoneNumber();
      
      String formattedValue = getFormattedValue();
      LOGGER.info("" + formattedValue);
      phoneNumber.setValue(formattedValue);

      return phoneNumber;
    }
  }

  public static class LocalPhoneNumberBuilder extends PhoneNumberBuilder {
    String subscriberNumber;
    String countryCode;
    String areaCode;
    String domainName;
    
    public LocalPhoneNumberBuilder(String subscriberNumber, String countryCode, String areaCode) {
      this.subscriberNumber = subscriberNumber;
      this.countryCode = countryCode;
      this.areaCode = areaCode;
    }

    public LocalPhoneNumberBuilder(String subscriberNumber, String domainName) {
      this.subscriberNumber = subscriberNumber;
      this.domainName = domainName;
    }

    public LocalPhoneNumberBuilder extension(String extension) {
      this.extension = extension;
      return this;
    }

    public LocalPhoneNumberBuilder subAddress(String subAddress) {
      this.subAddress = subAddress;
      return this;
    }

    public LocalPhoneNumberBuilder param(String name, String value) {
      super.setParam(name, value);
      return this;
    }
    
    public PhoneNumber build() throws PhoneNumberParseException {
      if (StringUtils.isBlank(subscriberNumber) || !subscriberNumber.matches(LOCAL_SUBSCRIBER_NUMBER_REGEX) ) {
        throw new IllegalArgumentException("LocalPhoneNumberBuilder subscriberNumber must contain only numeric characters and optional ., -, (, ) visual separator characters.");
      }
      
      this.number = subscriberNumber;

      if (StringUtils.isBlank(countryCode) && StringUtils.isBlank(domainName)) {
        throw new IllegalArgumentException("LocalPhoneNumberBuilder must have values for domainName or countryCode.");
      }
      
      if (StringUtils.isBlank(domainName)) {
        if (StringUtils.isBlank(countryCode) || !countryCode.matches(COUNTRY_CODE_REGEX)) {
          throw new IllegalArgumentException("LocalPhoneNumberBuilder countryCode must contain only numeric characters and an optional plus (+) prefix.");
        }
  
        if (areaCode != null && !StringUtils.isNumeric(areaCode)) {
          throw new IllegalArgumentException("LocalPhoneNumberBuilder areaCode must contain only numberic characters.");
        }
        
        if (!countryCode.startsWith(INTERNATIONAL_PREFIX)) {
          this.phoneContext = INTERNATIONAL_PREFIX + countryCode;
        } else {
          this.phoneContext = countryCode;
        }
                
        if (!StringUtils.isBlank(areaCode)) {
          this.phoneContext += (HYPHEN + areaCode);
        }
        
      } else {
        if (!domainName.matches(DOMAIN_NAME_REGEX)) {
          throw new IllegalArgumentException("LocalPhoneNumberBuilder domainName must contain only alphanumeric, . and - characters.");
        }
        
        this.phoneContext = domainName;
      }
      
      return super.build();
    }
  }

  public static class GlobalPhoneNumberBuilder extends PhoneNumberBuilder {
    String globalNumber;
    
    public GlobalPhoneNumberBuilder(String globalNumber) {
      this.globalNumber = globalNumber;
    }

    public GlobalPhoneNumberBuilder extension(String extension) {
      this.extension = extension;
      return this;
    }

    public GlobalPhoneNumberBuilder subAddress(String subAddress) {
      this.subAddress = subAddress;
      return this;
    }

    public GlobalPhoneNumberBuilder param(String name, String value) {
      super.setParam(name, value);
      return this;
    }
    
    public PhoneNumber build() throws PhoneNumberParseException {
      if (StringUtils.isBlank(globalNumber) || !globalNumber.matches(GLOBAL_NUMBER_REGEX)) {
        throw new IllegalArgumentException("GlobalPhoneNumberBuilder globalNumber must contain only numeric characters, optional ., -, (, ) visual separators, and an optional plus (+) prefix.");
      }

      if (globalNumber.startsWith(INTERNATIONAL_PREFIX)) {
        this.number = globalNumber;
      } else {
        this.number = INTERNATIONAL_PREFIX + globalNumber;
      }
      
      return super.build();
    }
  }

}
