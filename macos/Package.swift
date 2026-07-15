// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "MooToolNative",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .executable(name: "MooToolNative", targets: ["MooToolNativeApp"]),
        .library(name: "MooToolNativeCore", targets: ["MooToolNativeCore"])
    ],
    targets: [
        .target(name: "MooToolNativeCore"),
        .executableTarget(
            name: "MooToolNativeApp",
            dependencies: ["MooToolNativeCore"],
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "MooToolNativeCoreTests",
            dependencies: ["MooToolNativeCore"]
        )
    ]
)
