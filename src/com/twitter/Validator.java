
package com.twitter;

import java.net.*;
import java.util.*;
import java.text.*;

/**
 * A class to valide Tweets, usernames, lists, hashtags and URLs
 */
public class Validator {
  /** Minimum length of a string that can parse as a valid URL. Used for optimizations */
  private static final Integer MINIMUM_URL_LENGTH = 10; // "http://x.x"

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
    URL url;

    if (text != null && text.length() >= MINIMUM_URL_LENGTH) {
      try {
        url = new URL(text);
        // Do not allow for relative URLs
        return text.equals(url.toExternalForm());
      } catch (MalformedURLException exception) {
        // Ignored.
      }
    }
    return Boolean.FALSE;
  }
}