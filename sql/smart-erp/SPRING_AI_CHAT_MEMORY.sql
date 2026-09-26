create table `smart-erp`.SPRING_AI_CHAT_MEMORY
(
    conversation_id varchar(36)                                  not null,
    content         text                                         not null,
    type            enum ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') not null,
    timestamp       timestamp                                    not null,
    sequence_id     bigint                                       not null
);

create index SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_SEQUENCE_ID_IDX
    on `smart-erp`.SPRING_AI_CHAT_MEMORY (conversation_id, sequence_id);

create index SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
    on `smart-erp`.SPRING_AI_CHAT_MEMORY (conversation_id, timestamp);

