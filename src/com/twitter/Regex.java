
package com.twitter;

import java.util.*;
import java.util.regex.*;

public class Regex {
  private static final String[] RESERVED_ACTION_WORDS = {"twitter","lists",
  "retweet","retweets","following","followings","follower","followers",
  "with_friend","with_friends","statuses","status","activity","favourites",
  "favourite","favorite","favorites"};

  private static final String HASHTAG_CHARACTERS = "[a-z0-9_\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff]";

  /* URL extraction related hash regex collection */
  private static final String EXTRACT_URL_PRECEEDING_CHARS = "(?:[^\\-/\"':!=A-Z0-9_@＠]+|^|\\:)";
  private static final String EXTRACT_URL_DOMAIN = "(?:[^\\p{Punct}\\s][\\.-](?=[^\\p{Punct}\\s])|[^\\p{Punct}\\s]){1,}\\.[a-z]{2,}(?::[0-9]+)?";

  private static final String EXTRACT_URL_GENERAL_PATH_CHARS = "[a-z0-9!\\*';:=\\+\\$/%#\\[\\]\\-_,~\\|]";
  /** Allow extracted URL paths to contain balanced parens
   *  1. Used in Wikipedia URLs like /Primer_(film)
   *  2. Used in IIS sessions like /S(dfd346)/
  **/
  private static final String EXTRACT_URL_BALANCE_PARENS = "(?:\\(" + EXTRACT_URL_GENERAL_PATH_CHARS + "+\\))";
  private static final String EXTRACT_URL_PATH_CHARS = "(?:" +
    EXTRACT_URL_BALANCE_PARENS +
    "|@" + EXTRACT_URL_GENERAL_PATH_CHARS + "+/" +
    "|[\\.,]?" + EXTRACT_URL_GENERAL_PATH_CHARS + "+" +
  ")";

  /** Valid end-of-path chracters (so /foo. does not gobble the period).
   *   2. Allow =&# for empty URL parameters and other URL-join artifacts
  **/
  private static final String EXTRACT_URL_PATH_ENDING_CHARS = "(?:[a-z0-9=_#/\\-\\+]+|"+EXTRACT_URL_BALANCE_PARENS+")";
  private static final String EXTRACT_URL_QUERY_CHARS = "[a-z0-9!\\*'\\(\\);:&=\\+\\$/%#\\[\\]\\-_\\.,~\\|]";
  private static final String EXTRACT_URL_QUERY_ENDING_CHARS = "[a-z0-9_&=#/]";
  private static final String EXTRACT_URL_PATTERN_STRING =
  "(" +                                                            //  $1 total match
    "(" + EXTRACT_URL_PRECEEDING_CHARS + ")" +                     //  $2 Preceeding chracter
    "(" +                                                          //  $3 URL
      "(https?://)" +                                              //  $4 Protocol
      "(" + EXTRACT_URL_DOMAIN + ")" +                             //  $5 Domain(s) and optional port number
      "(/" +
        "(?:" +
          EXTRACT_URL_PATH_CHARS + "+" +
            EXTRACT_URL_PATH_ENDING_CHARS + "|" +                  //     1+ path chars and a valid last char
          EXTRACT_URL_PATH_CHARS + "+" +
            EXTRACT_URL_PATH_ENDING_CHARS + "?|" +                 //     Optional last char to handle /@foo/ case
          EXTRACT_URL_PATH_ENDING_CHARS +                          //     Just a # case
        ")?" +
      ")?" +                                                       //  $6 URL Path and anchor
      "(\\?" + EXTRACT_URL_QUERY_CHARS + "*" +                     //  $7 Query String
              EXTRACT_URL_QUERY_ENDING_CHARS + ")?" +
    ")" +
  ")";

  private static String AT_SIGNS_CHARS = "@\uFF20";
  private static String LATIN_ACCENTS_CHARS = "\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff";

  /* These URL validation pattern strings are based on the ABNF from RFC 3986 */
  private static final String VALIDATE_URL_UNRESERVED = "[a-z0-9\\-._~]";
  private static final String VALIDATE_URL_PCT_ENCODED = "(?:%[0-9a-f]{2})";
  private static final String VALIDATE_URL_SUB_DELIMS = "[!$&'()*+,;=]";
  private static final String VALIDATE_URL_PCHAR = "(?:" +
                                                   VALIDATE_URL_UNRESERVED + "|" +
                                                   VALIDATE_URL_PCT_ENCODED + "|" +
                                                   VALIDATE_URL_SUB_DELIMS + "|" +
                                                   ":|@)";

  private static final String VALIDATE_URL_SCHEME = "(?:[a-z][a-z0-9+\\-.]*)";
  private static final String VALIDATE_URL_USERINFO = "(?:" +
                                                      VALIDATE_URL_UNRESERVED + "|" +
                                                      VALIDATE_URL_PCT_ENCODED + "|" +
                                                      VALIDATE_URL_SUB_DELIMS + "|" +
                                                      ":)*";

  private static final String VALIDATE_URL_DEC_OCTET =
       "(?:[0-9]|(?:[1-9][0-9])|(?:1[0-9]{2})|(?:2[0-4][0-9])|(?:25[0-5]))";
  private static final String VALIDATE_URL_IPV4 = "(?:" +
                                                  VALIDATE_URL_DEC_OCTET +
                                                  "(?:\\." + VALIDATE_URL_DEC_OCTET + "){3})";

  // Punting on real IPv6 validation for now
  private static final String VALIDATE_URL_IPV6 = "(?:\\[[a-f0-9:\\.]+\\])";

  // Also punting on IPvFuture for now
  private static final String VALIDATE_URL_IP = "(?:" +
                                                VALIDATE_URL_IPV4 + "|" +
                                                VALIDATE_URL_IPV6 + ")";

  // This is more strict than the rfc specifies
  private static final String VALIDATE_URL_DOMAIN_SEGMENT = "(?:[a-z0-9](?:[a-z0-9\\-]*[a-z0-9])?)";
  private static final String VALIDATE_URL_DOMAIN = "(?:" +
                                                    VALIDATE_URL_DOMAIN_SEGMENT +
                                                    "\\.?)+";

  private static final String VALIDATE_URL_HOST = "(?:" +
                                                  VALIDATE_URL_IP + "|" +
                                                  VALIDATE_URL_DOMAIN +
                                                  ")";

  // Unencoded internationalized domains
  private static final String VALIDATE_URL_UNICODE_DOMAIN_SEGMENT =
    "(?:(?:[a-z0-9]|[^\u0000-\u007F])(?:(?:[a-z0-9\\-]|[^\u0000-\u007F])*(?:[a-z0-9]|[^\u0000-\u007F]))?)";
  private static final String VALIDATE_URL_UNICODE_DOMAIN = "(?:" +
                                                             VALIDATE_URL_UNICODE_DOMAIN_SEGMENT +
                                                             "\\.?)+";

  private static final String VALIDATE_URL_UNICODE_HOST = "(?:" +
                                                          VALIDATE_URL_IP + "|" +
                                                          VALIDATE_URL_UNICODE_DOMAIN +
                                                          ")";

  private static final String VALIDATE_URL_PORT = "[0-9]{1,5}";

  private static final String VALIDATE_URL_AUTHORITY =
    "(?:(" + VALIDATE_URL_USERINFO + ")@)?" +    //  $1 userinfo
    "([^/?#:]+)" +                               //  $2 host
    "(?::(" + VALIDATE_URL_PORT + "))?";    //  $3 port

  private static final String VALIDATE_URL_PATH = "(/" + VALIDATE_URL_PCHAR + "*)*";
  private static final String VALIDATE_URL_QUERY = "(" + VALIDATE_URL_PCHAR + "|/|\\?)*";
  private static final String VALIDATE_URL_FRAGMENT = "(" + VALIDATE_URL_PCHAR + "|/|\\?)*";

  // Modified version of RFC 3986 Appendix B
  private static final String VALIDATE_URL_UNENCODED =
    "^" +                            //  $0 Full URL
    "(?:" +
      "([^:/?#]+):" +                //  $1 Scheme
    ")" +
    "(?://" +
      "([^/?#]*)" +                  //  $2 Authority
    ")" +
    "([^?#]*)" +                     //  $3 Path
    "(?:" +
      "\\?([^#]*)" +                 //  $4 Query
    ")?" +
    "(?:" +
      "#(.*)" +                      //  $5 Fragment
    ")?";

  /* Begin public constants */
  public static final Pattern AT_SIGNS = Pattern.compile("[" + AT_SIGNS_CHARS + "]");

  public static final Pattern SCREEN_NAME_MATCH_END = Pattern.compile("^(?:[" + AT_SIGNS_CHARS + LATIN_ACCENTS_CHARS + "]|://)");

  public static final Pattern AUTO_LINK_HASHTAGS = Pattern.compile("(^|[^0-9A-Z&/]+)(#|\uFF03)([0-9A-Z_]*[A-Z_]+" + HASHTAG_CHARACTERS + "*)", Pattern.CASE_INSENSITIVE);
  public static final int AUTO_LINK_HASHTAGS_GROUP_BEFORE = 1;
  public static final int AUTO_LINK_HASHTAGS_GROUP_HASH = 2;
  public static final int AUTO_LINK_HASHTAGS_GROUP_TAG = 3;

  public static final Pattern AUTO_LINK_USERNAMES_OR_LISTS = Pattern.compile("([^a-z0-9_]|^|RT:?)(" + AT_SIGNS + "+)([a-z0-9_]{1,20})(/[a-z][a-z0-9_\\-]{0,24})?", Pattern.CASE_INSENSITIVE);
  public static final int AUTO_LINK_USERNAME_OR_LISTS_GROUP_BEFORE = 1;
  public static final int AUTO_LINK_USERNAME_OR_LISTS_GROUP_AT = 2;
  public static final int AUTO_LINK_USERNAME_OR_LISTS_GROUP_USERNAME = 3;
  public static final int AUTO_LINK_USERNAME_OR_LISTS_GROUP_LIST = 4;

  public static final Pattern EXTRACT_URL = Pattern.compile(EXTRACT_URL_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
  public static final int EXTRACT_URL_GROUP_ALL          = 1;
  public static final int EXTRACT_URL_GROUP_BEFORE       = 2;
  public static final int EXTRACT_URL_GROUP_URL          = 3;
  public static final int EXTRACT_URL_GROUP_PROTOCOL     = 4;
  public static final int EXTRACT_URL_GROUP_DOMAIN       = 5;
  public static final int EXTRACT_URL_GROUP_PATH         = 6;
  public static final int EXTRACT_URL_GROUP_QUERY_STRING = 7;

  public static final Pattern EXTRACT_MENTIONS = Pattern.compile("(^|[^a-z0-9_])" + AT_SIGNS + "([a-z0-9_]{1,20})(?=(.|$))", Pattern.CASE_INSENSITIVE);
  public static final int EXTRACT_MENTIONS_GROUP_BEFORE = 1;
  public static final int EXTRACT_MENTIONS_GROUP_USERNAME = 2;
  public static final int EXTRACT_MENTIONS_GROUP_AFTER = 3;
  
  public static final Pattern EXTRACT_MENTIONED_LISTS = Pattern.compile("(^|[^a-z0-9_])" + AT_SIGNS + "([a-z0-9_]{1,20}/[a-z][a-z0-9_\\-]{0,24})", Pattern.CASE_INSENSITIVE);
  public static final int EXTRACT_MENTIONED_LIST_GROUP_BEFORE = 1;
  public static final int EXTRACT_MENTIONED_LIST_GROUP_USERNAME_AND_LIST = 2;

  public static final Pattern EXTRACT_REPLY = Pattern.compile("^(?:[" + com.twitter.regex.Spaces.getCharacterClass() + "])*" + AT_SIGNS + "([a-z0-9_]{1,20}).*", Pattern.CASE_INSENSITIVE);
  public static final int EXTRACT_REPLY_GROUP_USERNAME = 1;

  public static final Pattern VALIDATE_URL_UNENCODED_PATTERN =
      Pattern.compile(VALIDATE_URL_UNENCODED, Pattern.CASE_INSENSITIVE);
  public static final Pattern VALIDATE_URL_SCHEME_PATTERN =
      Pattern.compile(VALIDATE_URL_SCHEME, Pattern.CASE_INSENSITIVE);
  public static final Pattern VALIDATE_URL_AUTHORITY_PATTERN =
      Pattern.compile(VALIDATE_URL_AUTHORITY, Pattern.CASE_INSENSITIVE);
  public static final Pattern VALIDATE_URL_USERINFO_PATTERN =
      Pattern.compile(VALIDATE_URL_USERINFO, Pattern.CASE_INSENSITIVE);
  public static final Pattern VALIDATE_URL_HOST_PATTERN =
      Pattern.compile(VALIDATE_URL_HOST, Pattern.CASE_INSENSITIVE);
  public static final Pattern VALIDATE_URL_UNICODE_HOST_PATTERN =
    Pattern.compile(VALIDATE_URL_UNICODE_HOST, Pattern.CASE_INSENSITIVE);
  public static final Pattern VALIDATE_URL_PORT_PATTERN =
      Pattern.compile(VALIDATE_URL_PORT, Pattern.CASE_INSENSITIVE);
  public static final Pattern VALIDATE_URL_PATH_PATTERN =
      Pattern.compile(VALIDATE_URL_PATH, Pattern.CASE_INSENSITIVE);
  public static final Pattern VALIDATE_URL_QUERY_PATTERN =
      Pattern.compile(VALIDATE_URL_QUERY, Pattern.CASE_INSENSITIVE);
  public static final Pattern VALIDATE_URL_FRAGMENT_PATTERN =
      Pattern.compile(VALIDATE_URL_FRAGMENT, Pattern.CASE_INSENSITIVE);

  public static final int VALIDATE_URL_UNENCODED_GROUP_ALL = 0;
  public static final int VALIDATE_URL_UNENCODED_GROUP_SCHEME = 1;
  public static final int VALIDATE_URL_UNENCODED_GROUP_AUTHORITY = 2;
  public static final int VALIDATE_URL_UNENCODED_GROUP_PATH = 3;
  public static final int VALIDATE_URL_UNENCODED_GROUP_QUERY = 4;
  public static final int VALIDATE_URL_UNENCODED_GROUP_FRAGMENT = 5;

  public static final int VALIDATE_URL_AUTHORITY_GROUP_ALL = 0;
  public static final int VALIDATE_URL_AUTHORITY_GROUP_USERINFO = 1;
  public static final int VALIDATE_URL_AUTHORITY_GROUP_HOST = 2;
  public static final int VALIDATE_URL_AUTHORITY_GROUP_PORT = 3;
}
