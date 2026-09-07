# 🍎 Guia Prático de Endurecimento e Minimização de Rastros — iOS
**Privacidade Forte por Design • Edição Brasil & LGPD (2026)**

---

> ### ⚖️ Termos de Uso e Isenção de Responsabilidade (Disclaimer)
> **Finalidade Educativa e Informativa**: Este material foi elaborado exclusivamente para fins de conscientização e orientação técnica de higiene digital. Não constitui assessoria jurídica, parecer regulatório ou garantia de segurança cibernética incondicional. A segurança e a privacidade são processos contínuos de redução de riscos; **nenhuma configuração ou ferramenta garante invisibilidade ou "zero-trace absoluto"** em redes públicas ou sistemas operacionais conectados.  
> **Licença de Distribuição**: É permitida a cópia, compartilhamento e distribuição gratuita deste documento em sua versão integral e sem alterações para fins pessoais e educativos, citando a fonte original (**Raix — raixtech.com**). É expressamente proibida a modificação, comercialização ou inclusão em serviços pagos sem autorização prévia por escrito.

---

## 🎯 Checklist de Configuração Rápida (iOS 17 / 18)

Execute as etapas abaixo para reduzir a pegada de telemetria, anúncios cruzados e vazamentos de metadados no seu iPhone:

### 🚨 Nível 1: Ações Críticas (Redução Imediata de Rastros)
- [ ] **1. Bloquear Rastreamento entre Aplicativos (App Tracking Transparency - ATT)**
  - *Caminho*: `Ajustes > Privacidade e Segurança > Rastreamento`.
  - *Ação*: Desative **"Permitir que os Apps Peçam para Rastrear"**. Isso impede que aplicativos solicitem o identificador IDFA, zerando-o e bloqueando a correlação de atividades entre empresas distintas.
- [ ] **2. Cortar Acesso Contínuo à Localização e "Localização Precisa"**
  - *Caminho*: `Ajustes > Privacidade e Segurança > Serviços de Localização`.
  - *Ação*: Revise a lista de aplicativos e altere de *"Sempre"* para *"Durante o Uso do App"* ou *"Nunca"*. Para apps que não exigem precisão métrica (ex.: redes sociais, previsão do tempo), desative **"Localização Precisa"** (fornece apenas a cidade/bairro aproximado).
- [ ] **3. Desativar "Locais Importantes" (Significant Locations)**
  - *Caminho*: `Ajustes > Privacidade e Segurança > Serviços de Localização > Serviços do Sistema > Locais Importantes`.
  - *Ação*: Desative a chave e toque em **"Limpar Histórico"**. Essa função registra silenciosamente os endereços onde você reside, trabalha e seus padrões rotineiros de deslocamento.
- [ ] **4. Expurgar Geotags (GPS) de Fotos e Compartilhamento**
  - *Caminho*: `Ajustes > Privacidade e Segurança > Serviços de Localização > Câmera`.
  - *Ação*: Altere para *"Nunca"*. Ao compartilhar imagens existentes pelo app Fotos, toque em *"Opções"* no topo da folha de compartilhamento e desmarque a opção **"Localização"** antes do envio.

---

### ⚠️ Nível 2: Endurecimento de Rede, Navegação e Telemetria Apple
- [ ] **5. Ativar a Proteção de Atividade no Mail (Bloqueio de Pixel Espião)**
  - *Caminho*: `Ajustes > Apps > Mail > Proteção de Privacidade` *(ou Ajustes > Mail > Proteção de Privacidade)*.
  - *Ação*: Ative **"Proteger Atividade no Mail"**. Isso oculta seu endereço IP real de remetentes de e-mail e impede que pixels de rastreamento saibam quando e onde você abriu a mensagem.
- [ ] **6. Ocultar Endereço IP no Safari contra Rastreadores**
  - *Caminho*: `Ajustes > Apps > Safari > Ocultar Endereço IP` *(ou Ajustes > Safari)*.
  - *Ação*: Selecione **"De Rastreadores e Sites"** *(ou "De Rastreadores")*. Dificulta a formação de impressões digitais (*fingerprinting*) por corretores de navegação.
- [ ] **7. Desativar Compartilhamento de Análises e Telemetria com a Apple**
  - *Caminho*: `Ajustes > Privacidade e Segurança > Análise e Melhorias`.
  - *Ação*: Desative **"Compartilhar Análise do iPhone"**, *"Compartilhar com Desenvolvedores"* e *"Melhorar Siri e Ditado"*. Impede o envio diário de logs de diagnósticos e amostras de interação.
- [ ] **8. Restringir Visibilidade de AirDrop e NameDrop**
  - *Caminho*: `Ajustes > Geral > AirDrop`.
  - *Ação*: Altere para **"Apenas Contatos"** ou **"Recepção Desativada"**. Desative a opção *"Aproximar Dispositivos"* caso não queira transmissão por aproximação involuntária em locais públicos.

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
