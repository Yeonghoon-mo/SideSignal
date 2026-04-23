// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SideSignal",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .executable(name: "SideSignal", targets: ["SideSignal"])
    ],
    targets: [
        .executableTarget(
            name: "SideSignal",
            path: "Sources/SideSignal"
        )
    ]
)
