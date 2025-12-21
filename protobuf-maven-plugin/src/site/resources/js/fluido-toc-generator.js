/*
 * Copyright (C) 2023 Ashley Scopes
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
  const tocHtml = buildTocHtml(headingTree, 0);

  const heading = document.createElement("h4");
  const hr1 = document.createElement("hr");
  const hr2 = document.createElement("hr");
  const br1 = document.createElement("br");
  const br2 = document.createElement("br");
  heading.innerText = "Table of Contents";

  targetElement.appendChild(br1);
  targetElement.appendChild(hr1);
  targetElement.appendChild(heading);
  targetElement.appendChild(tocHtml);
  targetElement.appendChild(hr2);
  targetElement.appendChild(br2);
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
  const headings = section.querySelectorAll("h2, h3, h4, h5, h6");
  const roots = [];
  const stack = [];

  for (const heading of headings) {
    const headingLevel = buildHeadingLevel(heading);

    while (stack.length > 0 && stack[stack.length - 1].level >= headingLevel.level) {
      stack.pop();
    }

    if (stack.length > 0) {
      stack[stack.length - 1].children.push(headingLevel);
    } else {
      roots.push(headingLevel);
    }

    stack.push(headingLevel);
  }

  return roots;
}

function buildHeadingLevel(element) {
  const level = parseInt(element.tagName[1]);

  // We're using anchors.js now to generate the anchor IDs, but if we remove that or it doesn't
  // work properly, then we want to fall back to generating the anchors ourselves.
  let id = element.getAttribute("id");
  if (typeof(id) === "undefined") {
    id = `${element.tagName}-${element.innerText.replaceAll(/[^A-Za-z0-9-]/g, "-")}`.toLowerCase();
    element.setAttribute("id", id);
  }

  return {
    name: element.innerText,
    id: id,
    level: level,
    children: [],
  };
}

function buildTocHtml(roots, level) {
  const ol = document.createElement("ol");
  ol.setAttribute("style", `list-style-type: ${listStyle(level)}`);

  for (const root of roots) {
    const a = document.createElement("a");
    a.setAttribute("href", `#${root.id}`);
    a.innerText = root.name;

    const nestedList = buildTocHtml(root.children, level + 1);

    const li = document.createElement("li");
    li.appendChild(a);
    li.appendChild(nestedList);

    ol.appendChild(li);
  }

  return ol;
}

function listStyle(level) {
  return ["decimal", "lower-latin", "lower-roman"][level % 3];
}
