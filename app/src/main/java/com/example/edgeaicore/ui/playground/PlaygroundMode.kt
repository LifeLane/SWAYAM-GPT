package com.example.edgeaicore.ui.playground

enum class PlaygroundMode(
    val title: String,
    val description: String,
    val iconTag: String
) {
    GENERAL("General", "Direct sovereign AI reasoning, conversation, and planning", "Chat"),
    RESEARCH("Research", "Multi-source evidence grounding with Memory, Knowledge & RAG", "Search"),
    DOCUMENTS("Documents", "Document retrieval, semantic search & grounded citation", "Description"),
    MEMORY("Memory", "Encrypted personal memory retrieval, inspection & retention", "Psychology"),
    AGENTS("Agents", "Autonomous multi-step task execution governed by ToolGateway", "SmartToy")
}
