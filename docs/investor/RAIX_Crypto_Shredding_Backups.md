# Qualificação do Crypto-shredding e Janelas de Retenção (PITR)

Este documento certifica os limites e a eficácia técnica do "Crypto-shredding" implementado no Raix, visando transparência absoluta no processo de due diligence e compliance com a LGPD (Direito ao Esquecimento).

## 1. O Mecanismo de Crypto-shredding
O Raix utiliza **Crypto-shredding** como método principal para o "Vanish-After-Read". 
* Quando uma mensagem é lida (ou o TTL expira), a infraestrutura não faz um simples `soft-delete`. O Firebase executa a deleção física do documento contendo o Envelope Criptográfico (Ciphertext + DEK Envelopada).
* Simultaneamente, a Chave Privada (X25519) no dispositivo do usuário descartou a DEK efêmera da memória.
* **Efeito imediato:** O Ciphertext se torna uma sequência de bytes matematicamente irreversível ("criptograficamente triturada"), dado que a DEK de 256 bits foi destruída.

## 2. Janela de Retenção (Point-in-Time Recovery - PITR)
Embora o Crypto-shredding invalide os dados instantaneamente para acessos lógicos e vazamentos futuros, a governança de dados do Google Cloud Firestore (backend da Raix) mantém infraestrutura de disaster recovery.

**Transparência de Backups Físicos:**
* O Firestore está configurado com PITR (Point-in-Time Recovery) habilitado por padrão para proteção contra desastres.
* A janela padrão de retenção de PITR do Google Cloud é de **até 7 dias**.
* **Implicação técnica:** Durante esta janela de 7 dias contados a partir da exclusão, os bytes do Envelope (contendo o ciphertext inerte) ainda residem em blocos de storage do Google.

## 3. Qualificação Honesta (Overclaim Avoidance)
Para evitar "overclaim" de privacidade absoluta instantânea no storage de baixo nível:
1. O Crypto-shredding é absoluto no nível da aplicação e da rede. A DEK, após descartada da RAM do device, não pode ser recuperada via Firebase.
2. Não existe backup centralizado das chaves X25519 dos usuários (Elas nunca deixam o Keystore/Secure Enclave do hardware).
3. Portanto, mesmo que uma autoridade exija o restauro do Firestore através de um snapshot de PITR (dentro dos 7 dias), o material recuperado será um **Ciphertext irreversível**, visto que as chaves privadas para decifrar a DEK não estão no backup do Google.

## 4. Veredito Técnico
A deleção efêmera do Raix é **imediatamente irreversível do ponto de vista criptográfico**, ainda que o armazenamento físico dos dados obliterados seja expurgado da infraestrutura do Google somente ao fim do TTL do PITR (7 dias). A implementação cumpre o Art. 16 da LGPD, pois o dado remanescente no backup perde seu caráter de "dado pessoal" ao tornar-se irreversivelmente ininteligível.
