/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.quatresh.kotlin.grpc.api.generator.protoc

import com.google.protobuf.Descriptors.EnumValueDescriptor

/** Represents the unqualified name of a proto enum constant, in UPPER_UNDERSCORE. */
data class ProtoEnumValueName(val name: String) : CharSequence by name {
    val asConstantName: ConstantName
        get() = ConstantName(name)

    override fun toString() = name
}

/** Returns the name of a proto enum constant. */
val EnumValueDescriptor.protoEnumValueName: ProtoEnumValueName
    get() = ProtoEnumValueName(name)
