// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "MooToolNextNative",
    platforms: [.macOS(.v14)],
    products: [
        .executable(name: "MooToolNextNative", targets: ["MooToolNextApp"]),
        .executable(name: "MooToolJSONWorker", targets: ["MooToolJSONWorker"]),
        .library(name: "MooToolNextCore", targets: ["MooToolNextCore"])
    ],
    dependencies: [.package(url: "https://github.com/jpsim/Yams.git", exact: "6.2.2")],
    targets: [
        .target(name: "MooToolNextCore", dependencies: [.product(name: "Yams", package: "Yams")], resources: [.process("Resources")]),
        .executableTarget(name: "MooToolJSONWorker", dependencies: ["MooToolNextCore"]),
        .executableTarget(name: "MooToolNextApp", dependencies: ["MooToolNextCore"], resources: [.process("Resources")]),
        .testTarget(name: "MooToolNextCoreTests", dependencies: ["MooToolNextCore"])
    ]
)
