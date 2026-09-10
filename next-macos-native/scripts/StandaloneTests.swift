import Foundation

// Run the SAME XCTest test bodies with Command Line Tools, which ship no XCTest.
// Full Xcode continues to discover and run CoreTests.swift via `swift test`.
class XCTestCase {}
private var failures = 0
func XCTFail(_ message: String = "Assertion failed", file: StaticString = #filePath, line: UInt = #line) {
    failures += 1; fputs("FAIL \(file):\(line): \(message)\n", stderr)
}
func XCTAssertTrue(_ expression: @autoclosure () throws -> Bool, _ message: String = "", file: StaticString = #filePath, line: UInt = #line) {
    do { if try !expression() { XCTFail(message.isEmpty ? "Expected true" : message, file: file, line: line) } } catch { XCTFail("\(error)", file: file, line: line) }
}
func XCTAssertFalse(_ expression: @autoclosure () throws -> Bool, _ message: String = "", file: StaticString = #filePath, line: UInt = #line) {
    do { if try expression() { XCTFail(message.isEmpty ? "Expected false" : message, file: file, line: line) } } catch { XCTFail("\(error)", file: file, line: line) }
}
func XCTAssertEqual<T: Equatable>(_ lhs: @autoclosure () throws -> T, _ rhs: @autoclosure () throws -> T, _ message: String = "", file: StaticString = #filePath, line: UInt = #line) {
    do { let a = try lhs(), b = try rhs(); if a != b { XCTFail("\(message) Expected \(b), got \(a)", file: file, line: line) } } catch { XCTFail("\(error)", file: file, line: line) }
}
func XCTAssertEqual(_ lhs: @autoclosure () throws -> Double, _ rhs: Double, accuracy: Double, file: StaticString = #filePath, line: UInt = #line) {
    do { let a = try lhs(); if abs(a - rhs) > accuracy { XCTFail("Expected \(rhs), got \(a)", file: file, line: line) } } catch { XCTFail("\(error)", file: file, line: line) }
}
func XCTAssertNotEqual<T: Equatable>(_ lhs: @autoclosure () throws -> T, _ rhs: @autoclosure () throws -> T, file: StaticString = #filePath, line: UInt = #line) {
    do { if try lhs() == rhs() { XCTFail("Expected unequal values", file: file, line: line) } } catch { XCTFail("\(error)", file: file, line: line) }
}
func XCTAssertNil<T>(_ value: @autoclosure () throws -> T?, file: StaticString = #filePath, line: UInt = #line) {
    do { if try value() != nil { XCTFail("Expected nil", file: file, line: line) } } catch { XCTFail("\(error)", file: file, line: line) }
}
func XCTAssertThrowsError<T>(_ expression: @autoclosure () throws -> T, _ message: String = "", file: StaticString = #filePath, line: UInt = #line) {
    do { _ = try expression(); XCTFail("Expected an error. \(message)", file: file, line: line) } catch {}
}

@main struct StandaloneTests {
    static func main() async {
        let suite = CoreTests()
        let tests: [(String, () throws -> Void)] = [
            ("Attachment insertion", suite.testNoteAttachmentInsertionMatchesElectron),
            ("Attachment images and files", suite.testNoteImageValidationAndIndependentFiles),
            ("Attachment backup and restore", suite.testNoteAttachmentBackupRestoreAndFailureAtomicity),
            ("Attachment exports", suite.testNoteAttachmentExportCopiesAndMissingReferences),
            ("Markdown images", suite.testMarkdownImageBlocksAndLiteralCode),
            ("Quick Note quick replacements", suite.testQuickNoteAllQuickReplaceActionsAndSelection),
            ("Quick Note lists", suite.testQuickNoteListPrefixAndTextSemantics),
            ("Quick Note persistence", suite.testQuickNoteOptionsDocumentCopiesAndLegacyWorkspace),
            ("Markdown blocks and tables", suite.testMarkdownTablesListsFencesAndBounds),
            ("Native JSON formatting", suite.testJSONNativeFormattingOrderAndDuplicateKeys),
            ("Native JSONPath", suite.testJSONNativePathsMatchElectronQueries),
            ("Native JSON conversions", suite.testJSONNativeXMLAndJavaConversions),
            ("Native JSON escape and swap", suite.testJSONNativeEscapesAndKeyValueSwap),
            ("Native JSON find and replace", suite.testJSONNativeFindReplaceUnicodeAndFlags),
            ("Native JSON options", suite.testJSONNativeOptionsLegacyAndBackup),
            ("Vault hierarchy", suite.testVaultHierarchyMutationsAndIdentity),
            ("Vault validation", suite.testVaultRejectsCyclesConflictsAndForeignParents),
            ("Vault search and sort", suite.testVaultSearchSortAndExpansion),
            ("Vault atomic import", suite.testVaultBatchImportIsAtomicAndPreservesPaths),
            ("Vault import reader", suite.testVaultImportReaderUsesOnlySelectedUTF8Files),
            ("Editor state and legacy backup", suite.testEditorStateClampingAndVaultBackupCompatibility),
            ("Catalog", suite.testCatalogMatchesElectronToolIDs),
            ("JSON scalars", suite.testJSONScalarsAndEscapedStrings),
            ("JSON paths", suite.testJSONPathAndPointer),
            ("JSON structure", suite.testJSONStructureTypesPathsAndLimits),
            ("JSON formatting options", suite.testJSONFormattingOptionsPreserveStringContent),
            ("Encodings", suite.testEncodingRoundTrips),
            ("Digest vectors", suite.testKnownDigests),
            ("Authenticated encryption", suite.testAESGCMAuthenticatedRoundTrip),
            ("Regex", suite.testRegexUnicodeGroupsReplacementAndFlags),
            ("Configuration", suite.testConfigConversionsAndConflicts),
            ("XML", suite.testXMLDoesNotReadExternalEntities),
            ("Diff", suite.testDiff),
            ("Time", suite.testTimestampFractionsNegativeAndZones),
            ("Calculator", suite.testCalculatorPrecedenceAndFunctions),
            ("Cron", suite.testCronTimesAndValidation),
            ("Protobuf", suite.testProtobufBoundsAndTypes),
            ("PDF ranges", suite.testPDFPageRanges),
            ("HTTP validation", suite.testHTTPRequestValidation),
            ("HTTP parameters and cookies", suite.testHTTPParametersCookiesAndBodyModes),
            ("cURL round trip", suite.testCurlImportExportPreservesRequest),
            ("cURL query and validation", suite.testCurlQueryEncodingAndUnsupportedSyntax),
            ("Extended workspace", suite.testExtendedWorkspaceAndLegacyCompatibility),
            ("Workspace isolation", suite.testWorkspaceRoundTripAndProductIsolation)
        ]
        for (name, test) in tests {
            let before = failures
            do { try test() } catch { XCTFail("\(name): \(error)") }
            print("\(failures == before ? "PASS" : "FAIL"): \(name)")
        }
        do { try await suite.testProcessCapturesOutputAndTimeout() } catch { XCTFail("Process runner: \(error)") }
        do { try await suite.testLocalHTTPResponse() } catch { XCTFail("Local HTTP: \(error)") }
        do { try await suite.testHTTPRedirectPolicyAndSessionIsolation() } catch { XCTFail("HTTP redirects: \(error)") }
        do { try await suite.testJSONWorkerTimeoutCancellationAndIsolation() } catch { XCTFail("JSON worker: \(error)") }
        print("\(tests.count + 4) test groups completed; \(failures) failures")
        exit(failures == 0 ? 0 : 1)
    }
}
