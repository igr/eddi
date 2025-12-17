package dev.oblac.eddi.example.college.ui

import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Routing.pageIndex() {
    get("/") {
        call.respondHtml {
            head()
            body {
                div("container") {
                    h1 { +"College Management System" }
                    p { +"This is a simple Ktor application with HTML templating." }
                    div("menu") {
                        a(href = "/students.html") { +"List Students" }
                    }
                    div("menu") {
                        a(href = "/student-add.html") { +"Add a Student" }
                    }
                    div("menu") {
                        a(href = "/course-add.html") { +"Publish Course" }
                    }
                }
            }
        }
    }
}

fun HTML.head() {
    head {
        title { +"College Management System" }
        style()
    }
}

fun BODY.javascript(url: String) {
    script {
        unsafe {
            +"""
    const API_ENDPOINT = "http://localhost:8080/api/$url";                

    function formDataToJson(formData) {
      const obj = {};
      formData.forEach((value, key) => {
        if (obj[key] !== undefined) {
          if (!Array.isArray(obj[key])) {
            obj[key] = [obj[key]];
          }
          obj[key].push(value);
        } else {
          obj[key] = value;
        }
      });
      return obj;
    }
    async function submitForm(event) {
      event.preventDefault();

      const form = event.target;
      const statusEl = document.getElementById("status");
      const submitButton = form.querySelector("button[type='submit']");

      statusEl.textContent = "";
      statusEl.className = "status";

      // Gather form data
      const formData = new FormData(form);
      const bodyObj = formDataToJson(formData);

      // Disable while submitting
      submitButton.disabled = true;
      submitButton.textContent = "Sending...";

      try {
        const response = await fetch(API_ENDPOINT, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            // Add more headers here if your API needs them (e.g., Authorization)
          },
          body: JSON.stringify(bodyObj),
        });

        const isJson = response.headers
          .get("content-type")
          ?.includes("application/json");

        const data = isJson ? await response.json() : await response.text();

        if (!response.ok) {
          throw new Error(
            typeof data === "string"
              ? data
              : (data.message || "Request failed with status " + response.status)
          );
        }

        statusEl.textContent = "Successfully submitted!";
        statusEl.classList.add("success");

        form.reset();
        console.log("API response:", data);
      } catch (err) {
        console.error(err);
        statusEl.textContent = "Error: " + err.message;
        statusEl.classList.add("error");
      } finally {
        submitButton.disabled = false;
        submitButton.textContent = "Send";
      }
    }

    document.addEventListener('DOMContentLoaded', function() {
        document
            .getElementById("apiForm")
            .addEventListener("submit", submitForm);
    });
            """.trimIndent()
        }
    }
}

