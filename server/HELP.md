# Read Me First
The following was discovered as part of building this project:

* The original package name 'com.reme.re-me' is invalid and this project uses 'com.reme.re_me' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.1/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.1/gradle-plugin/packaging-oci-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.1/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Security](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)

### APNs push notifications

Copy `.env.example` to `.env` in the `server/` directory and set the database and
APNs values there. `.env` is git-ignored. Enable Push Notifications for the app's
App ID and provisioning profile, then create an APNs authentication key in the
Apple Developer account. Set `APNS_TEAM_ID`, `APNS_KEY_ID`, and
`APNS_PRIVATE_KEY_PATH` (the path to the `.p8` file). `APNS_BUNDLE_ID` defaults to
`com.reme.re-me`. Keep `APNS_USE_SANDBOX=true` for Xcode development builds; set
it to `false` for TestFlight or App Store builds. This must match the app's
`aps-environment` signing entitlement. Without APNs credentials, message delivery continues and push
notifications are disabled. The iOS app asks for notification permission after
login and registers device tokens automatically.

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
