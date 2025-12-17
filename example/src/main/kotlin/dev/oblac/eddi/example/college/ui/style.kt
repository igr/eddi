package dev.oblac.eddi.example.college.ui

import kotlinx.html.HEAD
import kotlinx.html.style

fun HEAD.style() {
    style {
        +"""
            body {
                font-family: Arial, sans-serif;
                max-width: 1024px;
                margin: 50px auto;
                padding: 20px;
                background-color: #f5f5f5;
            }
            .container {
                background: white;
                padding: 30px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            h1 {
                color: #333;
                border-bottom: 2px solid #4CAF50;
                padding-bottom: 10px;
            }
            .menu {
                background: #e8f5e9;
                padding: 15px;
                border-radius: 4px;
                margin-top: 20px;
            }
            a {
                color: #4CAF50;
                text-decoration: none;
                padding: 10px 15px;
            }
            a:hover {
                background-color: #45a049;
                color: white;
            }
            form input[type='text'] {
                width: calc(100% - 22px);
                padding: 10px;
                margin: 10px 0;
                border: 1px solid #ccc;
                border-radius: 4px;
            }
            form button[type='submit'] {
                background-color: #4CAF50;
                color: white;
                border: none;
                padding: 10px 20px;
                border-radius: 4px;
                cursor: pointer;
            }
            div#status {
                margin-top: 15px;
                font-weight: bold;
                color: #d32f2f;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 20px;
            }
            table, th, td {
                border: 1px solid #ddd;
            }
            th, td {
                padding: 12px;
                text-align: left;
            }
            th {
                background-color: #4CAF50;
                color: white;
            }            
        """.trimIndent()
    }
}