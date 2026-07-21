# Relocate WebClient Documentation to Fix Commit Check Errors

The file `WebClient.md` is currently located within the Java source directory (`app/src/main/java/com/example/carpoolclient/utils/`). This is likely causing "Commit Check" errors because static analysis tools (like Android Lint or the IDE's built-in code analyzer) expect only `.java` or `.kt` files in these package-indexed folders.

## User Review Required

> [!IMPORTANT]
> I am assuming that the "commit checks" are failing due to the incorrect placement of the markdown documentation file within the Java source set. If there is a specific error message you are seeing, please share it.

## Proposed Changes

### [Documentation Management]

#### [DELETE] [WebClient.md](file:///C:/Users/Admin/Documents/CarpoolClient/app/src/main/java/com/example/carpoolclient/utils/WebClient.md)
#### [NEW] [WebClient.md](file:///C:/Users/Admin/Documents/CarpoolClient/app/WebClient.md)

Move the file to the `app/` module root directory, consistent with other documentation files in the project (like `endpointsDoc.md`).

## Verification Plan

### Manual Verification
- Verify that the file `app/WebClient.md` exists and contains the original documentation.
- Verify that the file is no longer present in `app/src/main/java/com/example/carpoolclient/utils/`.
- Ask the user to attempt the commit again to see if the error persists.
