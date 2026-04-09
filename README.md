# Sistema de Corretora Distribuída (Java RMI)

Este projeto é um sistema distribuído de simulação de Bolsa de Valores desenvolvido em Java, utilizando a tecnologia **RMI (Remote Method Invocation)**. A arquitetura é baseada no modelo Cliente-Servidor.

## 🚀 Funcionalidades

O sistema permite que múltiplos investidores conectados via rede interajam com o mercado de ações em tempo real.
As principais funções disponíveis no painel do cliente são:
1. **Listar Ações**: Visualização do preço e situação atual de todos os ativos.
2. **Consultar Preço**: Busca de uma ação específica através de sua Sigla (Ex: PETR4).
3. **Atualizar Preço**: Simulação de alta ou baixa no mercado do valor das ações.
4. **Criar Nova Ação**: Lançamento de um novo ativo no mercado (IPO).
5. **Excluir Ação**: Remoção total do ativo do servidor.

## 🛡️ Principais Diferenciais
- **Atualização em Tempo Real**: Usando *callbacks*, toda a rede é notificada instantaneamente quando um usúario faz alguma alteração, exclusão ou criação de ação.
- **Persistência de Dados**: O servidor grava automaticamente todas as ações em arquivo de disco (`bd_corretora.dat`). Caso ele seja desligado, todas as transações são recarregadas na volta.
- **Detector de Conexões**: Um sistema de ping do servidor identifica usuários que fecharam o programa brutalmente (Alt+F4 / Ctrl+C) e remove o IP deles da conexão, notificando a rede.
- **Filtros e Tratativas**: Impede o cadastro de valores negativos, criação de ações formadas apenas por números e bloqueia o plágio (cadastros duplicados).

## 🛠️ Como Executar o Projeto

Para executar, é recomendado inicializar terminais independentes para o Servidor e para cada um dos Clientes. Dentro do diretório raiz do projeto:

### 1. Compilação Completa
Primeiramente, compile os códigos Java da pasta raiz:
```bash
javac src/bolsa/*.java
```

### 2. Iniciando o Servidor (Host)
Execute o comando abaixo e aperte `ENTER` quando ele pedir o IP:
```bash
java -cp src bolsa.BolsaServer
```
O servidor será erguido e informará no terminal o **IP DO SERVIDOR** que deverá ser enviado aos clientes.

### 3. Iniciando os Clientes
Na mesma máquina ou em outras máquinas na mesma rede LAN / Hamachi, execute:
```bash
java -cp src bolsa.BolsaClient
```
Assim que o programa for aberto, insira o número de **IP DO SERVIDOR** e inicie as transações pelo terminal interativo colorido.
