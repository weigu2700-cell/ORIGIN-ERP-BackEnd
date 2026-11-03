-- RAG 知识库草稿表：与 org.smart.erp.ai.rag.RagDocument / RagChunk 实体对应。
-- 单租户模式，实体未声明 tenantId，故不引入 tenant_id 列；多租户接管时由 T4 隔离方案补充。
CREATE TABLE rag_document (
  id BIGINT NOT NULL PRIMARY KEY,
  doc_name VARCHAR(512) NULL,
  file_type VARCHAR(64) NULL,
  chunk_count INT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  uploaded_by VARCHAR(64) NULL,
  create_time TIMESTAMP(6) NULL,
  update_time TIMESTAMP(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rag_chunk (
  id BIGINT NOT NULL PRIMARY KEY,
  document_id BIGINT NOT NULL,
  chunk_seq INT NULL,
  content TEXT NULL,
  metadata_json TEXT NULL,
  embedding LONGTEXT NULL,
  embedding_dim INT NULL,
  create_time TIMESTAMP(6) NULL,
  KEY ix_rag_chunk_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
