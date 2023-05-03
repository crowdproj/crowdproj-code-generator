# Modular Kotlin code generator based on OpenAPI

This code generates kotlin code in modular style that is used in CrowdProj projects. Right now (version 0.0.6) onlyAPI
models generation is fixed in respect to default OpenAPI generators. So, the OpenAPI specs with discriminators are now
correctly generated with the sealed classes and correct discriminator field. The generated models use Kotlin
multiplatform kotlinx.serialization library and are prepared for the multiplatform projects.

## Roadmap

The further plans include:
1. Generation of the internal models
2. Support several backend frameworks
3. Generation of the storage models together with different databases repositories.

## Usage

See the example of usege in [the test project](crowdproj-generator-test). 

## License

Copyright 2023 CrowdProj team
Copyright 2023 Sergey Okatov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
