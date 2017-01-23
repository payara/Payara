/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fish.payara.micro.boot.loader.jar;

/**
 * Interface that can be used to filter and optionally rename jar entries.
 *
 * @author Phillip Webb
 */
interface JarEntryFilter {

	/**
	 * Apply the jar entry filter.
	 * @param name the current entry name. This may be different that the original entry
	 * name if a previous filter has been applied
	 * @return the new name of the entry or {@code null} if the entry should not be
	 * included.
	 */
	AsciiBytes apply(AsciiBytes name);

}
