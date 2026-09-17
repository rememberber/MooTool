# Electron `protobufTools.test.ts` → Compose 对照

| 场景 | Electron `protobufTools.test.ts` | Compose |
| --- | --- | --- |
| json-hex-round-trip | `converts JSON to protobuf Hex/Base64 and back` | `ProtobufEngineTest.convertsJsonToHexAndBackLikeElectron` |
| wire-decode | `decodes wire fields without a proto definition` | `ProtobufEngineTest.decodesWireFieldsWithoutADefinition` |
| format-proto | `formats compact proto definitions` | `ProtobufEngineTest.formatsCompactProtoDefinitions` |

Person fixture：`syntax = "proto3"; message Person { string name = 1; int32 age = 2; repeated string tags = 3; }`，JSON `{"name":"Moo","age":25,"tags":["desktop","tool"]}`。
