
package com.twitter;

import java.util.*;
import java.util.regex.*;
import java.text.*;

/**
 * A class to valide Tweets, usernames, lists, hashtags and URLs
 */
public class Validator {
  /** Minimum length of a string that can parse as a valid URL. Used for optimizations */
  private static final Integer MINIMUM_URL_LENGTH = 10; // "http://x.x"
  private static final Set<String> VALID_SCHEMES =
    new HashSet<String>(Arrays.asList("http", "https"));

  /** Extractor instance used to maintain compatibility */
  private final Extractor extractor;

  /** Default constructor */
  public Validator() {
    extractor = new Extractor();
  }

  public Boolean isValidTweet(String text) {
    // TODO: This assumes pre-normalized text. Adding normalization requires either ICU4J or Java 1.6
    if (text.length() > 140) {
      return Boolean.FALSE;
    }

    // Disallow BOM, special (U+FFFF) and direction change characters. Do this in a single pass of the string.
    CharacterIterator iter = new StringCharacterIterator(text);
    for (char currentChar = iter.first(); currentChar != CharacterIterator.DONE; currentChar = iter.next()) {
      if (currentChar == '\uFFFE' || currentChar == '\uFEFF' || currentChar == '\uFFFF' ||
          currentChar == '\u202A' || currentChar == '\u202B' || currentChar == '\u202C' ||
          currentChar == '\u202D' || currentChar == '\u202E') {
        return Boolean.FALSE;
      }
    }

    return Boolean.TRUE;
  }

  public Boolean isValidUsername(String text) {
    if (text != null && text.length() > 1) {
       String extracted = extractor.extractReplyScreenname(text);
       if (extracted != null) {
         // Use substring to skip the "@"
         return extracted.equals(text.substring(1));
       }
    }

    return Boolean.FALSE;
  }

  public Boolean isValidList(String text) {
    if (text != null && text.length() > 1) {
      List<String> extracted = extractor.extractMentionedLists(text);
      if (extracted != null && extracted.size() == 1) {
        String extractedList = extracted.get(0);
        if (extractedList != null) {
          // substring(1) to skip the "@" in the originl text
          return extracted.get(0).equals(text.substring(1));
        }
      }
    }

    return Boolean.FALSE;  }

  public Boolean isValidHashtag(String text) {
    if (text != null && text.length() > 1) {
      List<String> extracted = extractor.extractHashtags(text);
      if (extracted != null && extracted.size() == 1) {
        String extractedHashtag = extracted.get(0);
        if (extractedHashtag != null) {
          // substring(1) to skip the "#" in the originl text
          return extracted.get(0).equals(text.substring(1));
        }
      }
    }

    return Boolean.FALSE;
  }

  public Boolean isValidURL(String text) {
    return validateURL(text, true);
  }

  public Boolean isValidASCIIURL(String text) {
    return validateURL(text, false);
  }

  private Boolean validateURL(String text, boolean allowUnicodeDomains) {
    if (text == null || text.length() < MINIMUM_URL_LENGTH) {
      return Boolean.FALSE;
    }

    Matcher urlMatcher = Regex.VALIDATE_URL_UNENCODED_PATTERN.matcher(text);
    if (!urlMatcher.matches()) {
      return Boolean.FALSE;
    }

    String scheme = urlMatcher.group(Regex.VALIDATE_URL_UNENCODED_GROUP_SCHEME);
    if (scheme == null ||
        !Regex.VALIDATE_URL_SCHEME_PATTERN.matcher(scheme).matches() ||
        !VALID_SCHEMES.contains(scheme.toLowerCase())) {
      return Boolean.FALSE;
    }

    String authority = urlMatcher.group(Regex.VALIDATE_URL_UNENCODED_GROUP_AUTHORITY);
    if (authority == null) {
      return Boolean.FALSE;
    }
    Matcher authorityMatcher = Regex.VALIDATE_URL_AUTHORITY_PATTERN.matcher(authority);
    if (!authorityMatcher.matches()) {
      return Boolean.FALSE;
    }

    String host = authorityMatcher.group(Regex.VALIDATE_URL_AUTHORITY_GROUP_HOST);
    if (host == null) {
      return Boolean.FALSE;
    }
    if (allowUnicodeDomains) {
      if(!Regex.VALIDATE_URL_UNICODE_HOST_PATTERN.matcher(host).matches()) {
        return Boolean.FALSE;
      }
    } else if (!Regex.VALIDATE_URL_HOST_PATTERN.matcher(host).matches()) {
      return Boolean.FALSE;
    }

    String path = urlMatcher.group(Regex.VALIDATE_URL_UNENCODED_GROUP_PATH);
    if (path == null || !Regex.VALIDATE_URL_PATH_PATTERN.matcher(path).matches()) {
      return Boolean.FALSE;
    }

    String query = urlMatcher.group(Regex.VALIDATE_URL_UNENCODED_GROUP_QUERY);
    // optional query
    if (query != null && !Regex.VALIDATE_URL_QUERY_PATTERN.matcher(query).matches()) {
      return Boolean.FALSE;
    }

    String fragment = urlMatcher.group(Regex.VALIDATE_URL_UNENCODED_GROUP_FRAGMENT);
    // check optional fragment
    if (fragment != null && !Regex.VALIDATE_URL_FRAGMENT_PATTERN.matcher(fragment).matches()) {
      return Boolean.FALSE;
    }
    return Boolean.TRUE;
  }
}