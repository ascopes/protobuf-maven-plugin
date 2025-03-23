/*
 * Copyright (C) 2023 - 2025, Ashley Scopes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";

window.addEventListener("load", () => {
  initAnchorJs();
  initTableOfContents();
  initHighlightJs();
});

function initAnchorJs() {
  anchors.add("main h1, main h2, main h3, main h4, main h5, main h6");
}

function initTableOfContents() {
  generateTableOfContents();
}

function initHighlightJs() {
  hljs.addPlugin(new CopyButtonPlugin());
  hljs.highlightAll();
}
