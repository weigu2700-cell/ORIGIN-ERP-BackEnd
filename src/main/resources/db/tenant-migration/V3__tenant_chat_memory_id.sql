-- Server-generated tenant/user/conversation IDs may exceed the former 36 characters.
ALTER TABLE SPRING_AI_CHAT_MEMORY MODIFY COLUMN conversation_id VARCHAR(192) NOT NULL;
-- Preserve existing numeric conversation memories only when the local owner is known.
UPDATE SPRING_AI_CHAT_MEMORY m
JOIN ai_conversation c ON m.conversation_id=CAST(c.id AS CHAR)
JOIN sys_tenant_identity t ON t.singleton=1
SET m.conversation_id=CONCAT('t:',t.tenant_id,':u:',c.user_id,':c:',c.id)
WHERE m.conversation_id REGEXP '^[1-9][0-9]*$';
