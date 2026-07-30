/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.readmediagrams.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StructurizrToMermaidTest {

    @Test
    fun `context level workspace transpiles to C4Context`() {
        val dsl = """
            workspace "Bank" {
                model {
                    customer = person "Customer" "A bank customer"
                    bank = softwareSystem "Banking System" "Handles accounts"
                    customer -> bank "Uses" "HTTPS"
                }
                views {
                    systemContext bank {
                        include *
                        autoLayout
                    }
                }
            }
        """.trimIndent()
        val result = StructurizrToMermaid.transpile(dsl)
        assertTrue(result.mermaid.startsWith("C4Context"))
        assertTrue(result.mermaid.contains("Person(customer, \"Customer\", \"A bank customer\")"))
        assertTrue(result.mermaid.contains("System(bank, \"Banking System\", \"Handles accounts\")"))
        assertTrue(result.mermaid.contains("Rel(customer, bank, \"Uses\", \"HTTPS\")"))
        assertTrue(result.warnings.isEmpty(), "unexpected warnings: ${result.warnings}")
    }

    @Test
    fun `containers produce C4Container with boundary`() {
        val dsl = """
            workspace {
                model {
                    u = person "User"
                    s = softwareSystem "Shop" {
                        web = container "Web App" "Storefront" "React"
                        db = container "Database" "Orders" "PostgreSQL"
                    }
                    u -> web "Browses"
                    web -> db "Reads/writes" "SQL"
                }
            }
        """.trimIndent()
        val m = StructurizrToMermaid.transpile(dsl).mermaid
        assertTrue(m.startsWith("C4Container"))
        assertTrue(m.contains("System_Boundary(s, \"Shop\")"))
        assertTrue(m.contains("Container(web, \"Web App\", \"React\", \"Storefront\")"))
        assertTrue(m.contains("Rel(web, db, \"Reads/writes\", \"SQL\")"))
    }

    @Test
    fun `components produce C4Component with container boundary`() {
        val dsl = """
            model {
                s = softwareSystem "Sys" {
                    api = container "API" {
                        ctrl = component "Controller" "Handles HTTP" "Spring MVC"
                    }
                }
            }
        """.trimIndent()
        val m = StructurizrToMermaid.transpile(dsl).mermaid
        assertTrue(m.startsWith("C4Component"))
        assertTrue(m.contains("Container_Boundary(api, \"API\")"))
        assertTrue(m.contains("Component(ctrl, \"Controller\", \"Spring MVC\", \"Handles HTTP\")"))
    }

    @Test
    fun `anonymous elements get ids from names`() {
        val dsl = """
            model {
                person "Admin User"
                softwareSystem "Core"
            }
        """.trimIndent()
        val m = StructurizrToMermaid.transpile(dsl).mermaid
        assertTrue(m.contains("Person(Admin_User, \"Admin User\")"))
        assertTrue(m.contains("System(Core, \"Core\")"))
    }

    @Test
    fun `styles views and comments are skipped without warnings`() {
        val dsl = """
            workspace {
                // comment
                model {
                    a = person "A"
                }
                views {
                    styles {
                        element "Person" {
                            background #08427b
                        }
                    }
                }
            }
        """.trimIndent()
        val result = StructurizrToMermaid.transpile(dsl)
        assertTrue(result.warnings.isEmpty(), "unexpected: ${result.warnings}")
        assertTrue(result.mermaid.contains("Person(a, \"A\")"))
    }

    @Test
    fun `unsupported lines produce warnings not failures`() {
        val dsl = """
            model {
                a = person "A"
                healthCheck "weird" 42
            }
        """.trimIndent()
        val result = StructurizrToMermaid.transpile(dsl)
        assertTrue(result.mermaid.contains("Person(a"))
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun `empty dsl reports no elements`() {
        val result = StructurizrToMermaid.transpile("views { }")
        assertEquals("", result.mermaid)
        assertTrue(result.warnings.any { it.contains("No Structurizr elements") })
    }

    @Test
    fun `dotted relationship identifiers are sanitized`() {
        val dsl = """
            model {
                s = softwareSystem "S" {
                    web = container "Web"
                }
                u = person "U"
                u -> s.web "Uses"
            }
        """.trimIndent()
        val m = StructurizrToMermaid.transpile(dsl).mermaid
        assertTrue(m.contains("Rel(u, s_web, \"Uses\")"))
    }

    @Test
    fun `quotes in names are converted not broken`() {
        val dsl = """model { a = person "The \"Boss\"" }"""
        val m = StructurizrToMermaid.transpile(dsl).mermaid
        assertFalse(m.contains("\"The \"Boss\"\""))
    }

    @Test
    fun `group blocks are transparent`() {
        val dsl = """
            model {
                group "Team A" {
                    a = person "A"
                }
            }
        """.trimIndent()
        val m = StructurizrToMermaid.transpile(dsl).mermaid
        assertTrue(m.contains("Person(a, \"A\")"))
    }
}
