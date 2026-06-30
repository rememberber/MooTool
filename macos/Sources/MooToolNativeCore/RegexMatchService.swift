import Foundation

public enum RegexMatchError: Error, Equatable, Sendable {
    case invalidPattern
}

public struct RegexEvaluationResult: Equatable, Sendable {
    public let matches: [RegexMatch]

    public init(matches: [RegexMatch]) {
        self.matches = matches
    }
}

public struct RegexMatch: Equatable, Sendable {
    public let text: String
    public let range: NSRange
    public let captureGroups: [String]

    public init(text: String, range: NSRange, captureGroups: [String]) {
        self.text = text
        self.range = range
        self.captureGroups = captureGroups
    }
}

public enum RegexMatchService {
    public static func evaluate(pattern: String, input: String) throws -> RegexEvaluationResult {
        guard !pattern.isEmpty else {
            return RegexEvaluationResult(matches: [])
        }

        let expression: NSRegularExpression
        do {
            expression = try NSRegularExpression(pattern: pattern)
        } catch {
            throw RegexMatchError.invalidPattern
        }

        let nsInput = input as NSString
        let fullRange = NSRange(location: 0, length: nsInput.length)
        let matches = expression.matches(in: input, range: fullRange).map { match in
            RegexMatch(
                text: nsInput.substring(with: match.range),
                range: match.range,
                captureGroups: captureGroups(from: match, in: nsInput)
            )
        }
        return RegexEvaluationResult(matches: matches)
    }

    private static func captureGroups(from match: NSTextCheckingResult, in input: NSString) -> [String] {
        guard match.numberOfRanges > 1 else {
            return []
        }

        return (1..<match.numberOfRanges).compactMap { index in
            let range = match.range(at: index)
            guard range.location != NSNotFound else {
                return nil
            }
            return input.substring(with: range)
        }
    }
}
