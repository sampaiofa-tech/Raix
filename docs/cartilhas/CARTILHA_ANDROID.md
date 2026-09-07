# 📱 Guia Prático de Endurecimento e Minimização de Rastros — Android
**Privacidade Forte por Design • Edição Brasil & LGPD (2026)**

---

> ### ⚖️ Termos de Uso e Isenção de Responsabilidade (Disclaimer)
> **Finalidade Educativa e Informativa**: Este material foi elaborado exclusivamente para fins de conscientização e orientação técnica de higiene digital. Não constitui assessoria jurídica, parecer regulatório ou garantia de segurança cibernética incondicional. A segurança e a privacidade são processos contínuos de redução de riscos; **nenhuma configuração ou ferramenta garante invisibilidade ou "zero-trace absoluto"** em redes públicas ou sistemas operacionais conectados.  
> **Licença de Distribuição**: É permitida a cópia, compartilhamento e distribuição gratuita deste documento em sua versão integral e sem alterações para fins pessoais e educativos, citando a fonte original (**Raix — raixtech.com**). É expressamente proibida a modificação, comercialização ou inclusão em serviços pagos sem autorização prévia por escrito.

---

## 🎯 Checklist de Configuração Rápida (Android 14 / 15)

Execute as etapas abaixo para reduzir drasticamente a coleta passiva de dados e o rastreamento do seu dispositivo:

### 🚨 Nível 1: Ações Críticas (Redução Imediata de Rastros)
- [ ] **1. Cortar Localização em Segundo Plano ("Permitir o tempo todo")**
  - *Caminho*: `Configurações > Localização > Permissões de aplicativos`.
  - *Ação*: Altere de *"Permitir o tempo todo"* para *"Permitir apenas durante o uso"* em todos os aplicativos. Apenas apps estritamente necessários (ex.: navegação em tempo real) devem acessar sua posição geográfica.
- [ ] **2. Pausar Histórico de Localização do Google**
  - *Caminho*: `Configurações > Localização > Serviços de localização > Histórico de localização`.
  - *Ação*: Pause o Histórico de Localização e ative a exclusão automática para 3 meses.
- [ ] **3. Excluir o ID de Publicidade (Advertising ID / GAID)**
  - *Caminho*: `Configurações > Segurança e privacidade > Mais configurações de privacidade > Anúncios` *(ou Configurações > Google > Todos os serviços > Anúncios)*.
  - *Ação*: Toque em **"Excluir ID de publicidade"**. Isso remove o identificador único que corretores de dados e redes de anúncios usam para cruzar seus hábitos entre aplicativos distintos.
- [ ] **4. Expurgar Geotags (GPS) da Câmera Fotográfica**
  - *Caminho*: Abra o app `Câmera > Configurações (ícone ⚙️) > Salvar localização / Tags de localização`.
  - *Ação*: Desative a opção. Fotos com metadados EXIF/GPS expõem a latitude e longitude exatas de onde você mora, trabalha ou frequenta ao serem compartilhadas.

---

### ⚠️ Nível 2: Endurecimento de Rede, Sensores e Teclado
- [ ] **5. Desativar Escaneamento Contínuo por Wi-Fi e Bluetooth**
  - *Caminho*: `Configurações > Localização > Serviços de localização`.
  - *Ação*: Desative **"Busca por redes Wi-Fi"** e **"Busca por dispositivos Bluetooth"**. Impede que seu aparelho emita beacons e escaneie redes no bolso mesmo com Wi-Fi/Bluetooth desligados.
- [ ] **6. Ativar DNS Privado Criptografado (DNS-over-TLS)**
  - *Caminho*: `Configurações > Rede e internet > DNS privado`.
  - *Ação*: Selecione *"Nome do host do provedor de DNS privado"* e insira um resolver auditado sem logs (ex.: `dns.quad9.net` ou `family.cloudflare-dns.com` ou `dns.adguard.com`). Impede operadoras e roteadores Wi-Fi públicos de registrarem os domínios que você acessa.
- [ ] **7. Bloquear Telemetria e Aprendizado do Teclado Virtual**
  - *Caminho*: `Configurações > Sistema > Idiomas > Teclado na tela > Gboard > Privacidade`.
  - *Ação*: Desative *"Compartilhar estatísticas de uso"* e *"Personalizar para você / Melhorar para todos"*. Impede envio de amostras de digitação aos servidores da nuvem.
- [ ] **8. Revogação Automática de Permissões para Apps Dormentes**
  - *Caminho*: `Configurações > Apps > Acesso especial a apps` *(ou Permissões não usadas)*.
  - *Ação*: Ative a suspensão automática de permissões concedidas a apps que não são abertos há mais de 90 dias.

---

## 🇧🇷 Seus Direitos no Brasil (LGPD — Lei nº 13.709/2018)

A legislação brasileira garante controle sobre seus dados pessoais mantidos por empresas e instituições:

1. **Direitos Essenciais do Titular (Art. 18)**:
   - **Confirmação e Acesso**: Saber se uma empresa possui seus dados e solicitar cópia integral;
   - **Correção**: Retificar dados incompletos, inexatos ou desatualizados;
   - **Eliminação e Revogação**: Exigir a exclusão de dados tratados com seu consentimento ou de forma excessiva;
   - **Informação de Compartilhamento**: Saber com quais entidades públicas e privadas seus dados foram compartilhados.
2. **Como Exercer na Prática**:
   - Envie e-mail formal ao **Encarregado pelo Tratamento de Dados (DPO)** da organização (contato obrigatoriamente público na Política de Privacidade da empresa);
   - O fornecimento deve ser **gratuito** e respondido de forma simplificada em até **15 dias** (Art. 19, II);
   - Caso a empresa não responda ou recuse sem base legal idônea, faça uma petição formal perante a **Autoridade Nacional de Proteção de Dados (ANPD)** pelo canal oficial no portal `gov.br/anpd`.
3. **Data Brokers no Brasil (Birôs de Dados)**:
   - Os maiores birôs de crédito e agregadores de perfil cadastral operando no país incluem **Serasa Experian**, **Boa Vista SCPC**, além de brokers globais com filiais nacionais (Acxiom, CoreLogic, LexisNexis);
   - **Atenção**: *Não existe um cadastro centralizado único para exclusão no Brasil*. É necessário solicitar a exclusão ou opt-out de compartilhamento comercial individualmente em cada empresa e **repetir o processo a cada 3 a 6 meses**, pois novas bases de dados são adquiridas com frequência.

---
*Material informativo desenvolvido pelo time de engenharia da **Raix** • [raixtech.com](https://raixtech.com) • Versão 2026.1*
