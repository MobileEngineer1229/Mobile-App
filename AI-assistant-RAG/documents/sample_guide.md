# AI-assistant-RAG Sample Guide

AI-assistant-RAG is a small retrieval augmented generation chatbot. It reads
documents from the documents folder, stores searchable chunks in a SQLite vector
database, and answers questions by using the most relevant document passages.

The first version is designed to run locally without an external AI service.
It uses hashed text vectors and cosine similarity for retrieval. Answers are
short and include references to the source documents.

To add knowledge, place text, markdown, JSON, or JSONL files in the documents
folder. Then run the ingestion script again. The chatbot will use the updated
vector database the next time it answers a question.

For production use, the retrieval layer can stay the same while the final
answer writer can be replaced with a stronger local model or an API model.
