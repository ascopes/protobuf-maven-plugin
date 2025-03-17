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

function generateTableOfContents() {
  const targetElement = document.getElementById("pmp-toc");

  if (targetElement === null || typeof(targetElement) === "undefined") {
    return;
  }

  const section = getParentSection(targetElement);
  const headingTree = buildHeadingTree(section);
  targetElement.innerHTML = "<p>TODO: finish implementing this<p>";
}

function getParentSection(element) {
  do {
    element = element.parentElement;
  } while (element !== null && element.tagName !== "SECTION");

  if (element === null) {
    throw new Error("No parent section was found, panic!");
  }

  return element;
}

function buildHeadingTree(section) {
  // Purposely don't get the h1 heading for the page itself.
  const headings = section.querySelectorAll("h2, h3, h4, h5, h6");
  const headers = [];
  return "not implemented yet";
}

window.addEventListener("load", () => generateTableOfContents());
