package org.finos.fluxnova;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.TaskService;
import org.finos.fluxnova.bpm.engine.runtime.Execution;
import org.finos.fluxnova.bpm.engine.runtime.Job;
import org.finos.fluxnova.bpm.engine.runtime.ProcessInstance;
import org.finos.fluxnova.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProcessStartupRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessStartupRunner.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ManagementService managementService;

    public ProcessStartupRunner(RuntimeService runtimeService, TaskService taskService,
                                ManagementService managementService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.managementService = managementService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startProcessesOnReady() {
        cleanUpExistingInstances();
        startLoanInstances();
        startRiskInstances();
    }

    private void cleanUpExistingInstances() {
        for (String processKey : List.of("Process_LoanApproval", "Process_SpeculativeRisk")) {
            List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processKey)
                    .list();
            for (ProcessInstance instance : instances) {
                try {
                    runtimeService.deleteProcessInstance(instance.getId(), "server restart – seed data refresh");
                    LOG.info("Deleted stale {} instance {}", processKey, instance.getId());
                } catch (Exception e) {
                    LOG.warn("Could not delete instance {}: {}", instance.getId(), e.getMessage());
                }
            }
        }
    }

    private void startLoanInstances() {
        try {
            // ------------------------------------------------------------------
            // Instance 1: Awaiting clerk review at Check Documents
            // First-time residential buyer; utility bill date needs verification
            // ------------------------------------------------------------------
            Map<String, Object> inst1Vars = new HashMap<>();
            inst1Vars.put("applicantName", "James Thornton");
            inst1Vars.put("referenceNumber", "LA-2026-001847");
            inst1Vars.put("loanType", "Residential Mortgage");
            inst1Vars.put("propertyAddress", "14 Maple Close, Edinburgh EH6 5AZ");
            inst1Vars.put("loanTermYears", 25);
            inst1Vars.put("creditScore", 720);
            inst1Vars.put("loanAmount", 285000.0);
            inst1Vars.put("annualIncome", 68000.0);
            inst1Vars.put("existingMonthlyDebt", 420.0);
            inst1Vars.put("employmentStatus", "Employed");
            inst1Vars.put("loanPurpose", "Residential");
            inst1Vars.put("notes", "First-time buyer. Passport and payslips verified. Outstanding: utility bill on file is dated 4 months ago — clerk to confirm acceptability or request a more recent statement before advancing.");

            ProcessInstance inst1 = runtimeService.startProcessInstanceByKey("Process_LoanApproval", inst1Vars);
            LOG.info("Loan instance 1 (Check Documents – first-time buyer) – id: {}", inst1.getId());

            // ------------------------------------------------------------------
            // Instance 2: Employment eligibility check running
            // Fixed-term contract employee; short remaining tenure flagged
            // ------------------------------------------------------------------
            Map<String, Object> inst2Vars = new HashMap<>();
            inst2Vars.put("applicantName", "Priya Nair");
            inst2Vars.put("referenceNumber", "LA-2026-002103");
            inst2Vars.put("loanType", "Car Loan");
            inst2Vars.put("vehicleDescription", "2023 Toyota Yaris GR Sport");
            inst2Vars.put("contractType", "Fixed-Term");
            inst2Vars.put("contractEndDate", "2026-09-30");
            inst2Vars.put("creditScore", 620);
            inst2Vars.put("loanAmount", 24500.0);
            inst2Vars.put("annualIncome", 38000.0);
            inst2Vars.put("existingMonthlyDebt", 380.0);
            inst2Vars.put("employmentStatus", "Employed");
            inst2Vars.put("loanPurpose", "Car");
            inst2Vars.put("notes", "Fixed-term contract ending 30 Sep 2026 (6 months remaining). Employment eligibility check queued — short contract duration may prompt manual review of income continuity.");

            ProcessInstance inst2 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst2.getId(), "CheckDocs", inst2Vars);
            runtimeService.suspendProcessInstanceById(inst2.getId());
            LOG.info("Loan instance 2 (Employment Check – fixed-term contract) – id: {}", inst2.getId());

            // ------------------------------------------------------------------
            // Instance 3: Affordability check running
            // High monthly commitments; debt-to-income ratio flagged as marginal
            // ------------------------------------------------------------------
            Map<String, Object> inst3Vars = new HashMap<>();
            inst3Vars.put("applicantName", "David Walsh");
            inst3Vars.put("referenceNumber", "LA-2026-002289");
            inst3Vars.put("loanType", "Personal Loan");
            inst3Vars.put("existingDebtBreakdown", "Vehicle PCP £680/mo; BNPL/store finance £670/mo");
            inst3Vars.put("creditScore", 540);
            inst3Vars.put("loanAmount", 45000.0);
            inst3Vars.put("annualIncome", 55000.0);
            inst3Vars.put("existingMonthlyDebt", 1350.0);
            inst3Vars.put("employmentStatus", "Employed");
            inst3Vars.put("loanPurpose", "Personal");
            inst3Vars.put("notes", "High existing monthly obligations (vehicle PCP + BNPL). Pre-loan DTI ratio 29.5%. Adding the requested £45k loan repayment (~£890/mo over 5 years) would push total obligations to 49.1% of gross monthly income. Affordability model running — marginal outcome expected.");

            ProcessInstance inst3 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst3.getId(), "CheckDocs", inst3Vars);
            executeJobForActivity(inst3.getId(), "EmploymentCheck");
            runtimeService.suspendProcessInstanceById(inst3.getId());
            LOG.info("Loan instance 3 (Affordability Check – high commitments) – id: {}", inst3.getId());

            // ------------------------------------------------------------------
            // Instance 4: Compliance review stalled — AML/KYC screening pending
            // Self-employed director of BVI entity; enhanced due diligence triggered
            // ------------------------------------------------------------------
            Map<String, Object> inst4Vars = new HashMap<>();
            inst4Vars.put("applicantName", "Sophie Chen");
            inst4Vars.put("referenceNumber", "LA-2026-002541");
            inst4Vars.put("loanType", "Personal Loan");
            inst4Vars.put("businessName", "SCG Holdings Ltd (BVI-registered)");
            inst4Vars.put("overseasIncomeFlag", true);
            inst4Vars.put("pepScreeningStatus", "PENDING");
            inst4Vars.put("recentAddressChanges", 3);
            inst4Vars.put("creditScore", 460);
            inst4Vars.put("loanAmount", 250000.0);
            inst4Vars.put("annualIncome", 145000.0);
            inst4Vars.put("existingMonthlyDebt", 2100.0);
            inst4Vars.put("employmentStatus", "Self-Employed");
            inst4Vars.put("loanPurpose", "Personal");
            inst4Vars.put("notes", "Director of BVI-registered holding company with declared offshore income. Enhanced due diligence triggered: PEP screening result pending from Refinitiv World-Check. 3 address changes in 24 months and 2 large international inbound transfers (£85k, £62k) also flagged for AML review. Compliance team SLA: 5 business days.");

            ProcessInstance inst4 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst4.getId(), "CheckDocs", inst4Vars);
            executeJobForActivity(inst4.getId(), "EmploymentCheck");
            executeJobForActivity(inst4.getId(), "AffordabilityCheck");
            runtimeService.suspendProcessInstanceById(inst4.getId());
            LOG.info("Loan instance 4 (Compliance Review – AML/KYC screening pending) – id: {}", inst4.getId());

            // ------------------------------------------------------------------
            // Instance 5: Auto Decision queued — strong credit, clean profile
            // All eligibility and affordability checks passed; automated path
            // ------------------------------------------------------------------
            Map<String, Object> inst5Vars = new HashMap<>();
            inst5Vars.put("applicantName", "Alex Martinez");
            inst5Vars.put("referenceNumber", "LA-2026-002677");
            inst5Vars.put("loanType", "Car Loan");
            inst5Vars.put("vehicleDescription", "2024 BMW 3 Series M-Sport");
            inst5Vars.put("creditScore", 700);
            inst5Vars.put("loanAmount", 32000.0);
            inst5Vars.put("annualIncome", 92000.0);
            inst5Vars.put("existingMonthlyDebt", 200.0);
            inst5Vars.put("employmentStatus", "Employed");
            inst5Vars.put("loanPurpose", "Car");
            inst5Vars.put("employmentTenureYears", 6);
            inst5Vars.put("notes", "Exemplary credit profile. DTI ratio 2.6% well within thresholds. Permanent employment verified (6 years tenure, senior engineer). No adverse credit flags or CCJs. All automated checks returned pass status. Auto-decision service queued.");

            ProcessInstance inst5 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst5.getId(), "CheckDocs", inst5Vars);
            executeJobForActivity(inst5.getId(), "EmploymentCheck");
            executeJobForActivity(inst5.getId(), "AffordabilityCheck");
            executeJobForActivity(inst5.getId(), "ComplianceReview");
            LOG.info("Loan instance 5 (Auto Decision – strong credit profile) – id: {}", inst5.getId());

            // ------------------------------------------------------------------
            // Instance 6: Awaiting Manager Approval
            // Borderline credit score and recent late payments; discretionary review
            // ------------------------------------------------------------------
            Map<String, Object> inst6Vars = new HashMap<>();
            inst6Vars.put("applicantName", "Robert Okafor");
            inst6Vars.put("referenceNumber", "LA-2026-002814");
            inst6Vars.put("loanType", "Residential Mortgage");
            inst6Vars.put("propertyAddress", "8 Birchwood Avenue, Leicester LE3 9PQ");
            inst6Vars.put("creditScore", 400);
            inst6Vars.put("loanAmount", 195000.0);
            inst6Vars.put("annualIncome", 52000.0);
            inst6Vars.put("existingMonthlyDebt", 640.0);
            inst6Vars.put("employmentStatus", "Employed");
            inst6Vars.put("loanPurpose", "Personal");
            inst6Vars.put("adverseEntries", "2 late payments (Apr 2024, Jul 2024)");
            inst6Vars.put("managerSlaDeadline", "2026-03-15T17:00:00Z");
            inst6Vars.put("notes", "Credit score 400 routes application to manager discretionary review tier. Two recorded late payments in 2024 (since resolved). Stable employment as secondary school teacher. Manager must respond by 15 Mar 17:00 UTC or SLA breach will be triggered.");

            ProcessInstance inst6 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst6.getId(), "CheckDocs", inst6Vars);
            executeJobForActivity(inst6.getId(), "EmploymentCheck");
            executeJobForActivity(inst6.getId(), "AffordabilityCheck");
            executeJobForActivity(inst6.getId(), "ComplianceReview");
            LOG.info("Loan instance 6 (Manager Approval – borderline credit, late payments) – id: {}", inst6.getId());

            // ------------------------------------------------------------------
            // Instance 7: Awaiting Senior Manager Approval
            // High-value loan + low credit score + prior CCJ; highest risk tier
            // ------------------------------------------------------------------
            Map<String, Object> inst7Vars = new HashMap<>();
            inst7Vars.put("applicantName", "Laura Kovacevic");
            inst7Vars.put("referenceNumber", "LA-2026-003001");
            inst7Vars.put("loanType", "Commercial Loan");
            inst7Vars.put("companyName", "Kovacevic Property Developments Ltd");
            inst7Vars.put("creditScore", 350);
            inst7Vars.put("loanAmount", 600000.0);
            inst7Vars.put("annualIncome", 200000.0);
            inst7Vars.put("existingMonthlyDebt", 1000.0);
            inst7Vars.put("employmentStatus", "Self-Employed");
            inst7Vars.put("loanPurpose", "Personal");
            inst7Vars.put("adverseCreditFlag", true);
            inst7Vars.put("ccjDetails", "CCJ registered Aug 2022 (£14,500 — satisfied Nov 2022)");
            inst7Vars.put("existingMortgageCount", 4);
            inst7Vars.put("loanToValuePct", 68.0);
            inst7Vars.put("notes", "£600k commercial property acquisition loan. Credit score 350 with a satisfied CCJ from 2022 triggers senior manager approval tier. Applicant holds a portfolio of 4 existing mortgages (all active). Loan-to-value 68%. Senior manager sign-off required — full risk assessment attached to decision pack.");

            ProcessInstance inst7 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst7.getId(), "CheckDocs", inst7Vars);
            executeJobForActivity(inst7.getId(), "EmploymentCheck");
            executeJobForActivity(inst7.getId(), "AffordabilityCheck");
            executeJobForActivity(inst7.getId(), "ComplianceReview");
            LOG.info("Loan instance 7 (Senior Manager Approval – high-value loan + CCJ) – id: {}", inst7.getId());

        } catch (Exception e) {
            LOG.error("Failed to start loan instances: {}", e.getMessage(), e);
        }
    }

    private void startRiskInstances() {
        try {
            // ------------------------------------------------------------------
            // Instance 1: Trade captured — both user tasks open simultaneously
            // Large EUR/USD speculative forward; risk team inputs not yet submitted
            // ------------------------------------------------------------------
            Map<String, Object> inst1Vars = new HashMap<>();
            inst1Vars.put("tradeId", "TRD-2026-031347");
            inst1Vars.put("instrument", "EUR/USD Forward");
            inst1Vars.put("notionalAmount", 150000000.0);
            inst1Vars.put("currency", "EUR");
            inst1Vars.put("trader", "Chris Mercer");
            inst1Vars.put("tradeDesk", "EM FX Speculative Desk");
            inst1Vars.put("counterparty", "Goldman Sachs International");
            inst1Vars.put("maturityDate", "2026-06-13");
            inst1Vars.put("bookingTimestamp", "2026-03-13T08:47:22Z");
            inst1Vars.put("counterpartyCreditUtilisationPct", 78.0);
            inst1Vars.put("notes", "New large speculative EUR/USD 3-month forward position. Risk analyst and middle office tasks created in parallel — neither submitted yet. Counterparty credit limit 78% utilised; credit team flagged for monitoring.");

            ProcessInstance inst1 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk", inst1Vars);
            LOG.info("Risk instance 1 (trade captured – both tasks open) – id: {}", inst1.getId());

            // ------------------------------------------------------------------
            // Instance 2: Risk metrics specified; market data snapshot overdue
            // Analyst configured VaR parameters; middle office yet to respond
            // ------------------------------------------------------------------
            Map<String, Object> inst2BaseVars = new HashMap<>();
            inst2BaseVars.put("tradeId", "TRD-2026-031209");
            inst2BaseVars.put("instrument", "USD/JPY Interest Rate Swap");
            inst2BaseVars.put("notionalAmount", 75000000.0);
            inst2BaseVars.put("currency", "USD");
            inst2BaseVars.put("trader", "Natasha Volkov");
            inst2BaseVars.put("tradeDesk", "G10 Rates Desk");

            Map<String, Object> inst2MetricsVars = new HashMap<>();
            inst2MetricsVars.put("varConfidence", 0.99);
            inst2MetricsVars.put("varHorizon", 10);
            inst2MetricsVars.put("stressScenario", "BOJ_RATE_SHOCK_2024");
            inst2MetricsVars.put("riskAnalyst", "Raj Patel");
            inst2MetricsVars.put("metricsSubmittedAt", "2026-03-13T09:15:00Z");
            inst2MetricsVars.put("notes", "Risk metrics submitted at 09:15 by analyst Raj Patel (10-day VaR, 99% CI, BOJ rate shock scenario). Middle office market snapshot still outstanding — Bloomberg feed experiencing latency across EMEA session. SLA breach in 47 min.");

            ProcessInstance inst2 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk", inst2BaseVars);
            completeTaskWithVars(inst2.getId(), "Task_SpecifyRiskMetrics", inst2MetricsVars);
            LOG.info("Risk instance 2 (risk metrics set; market data overdue) – id: {}", inst2.getId());

            // ------------------------------------------------------------------
            // Instance 3: Market data filed; risk metrics still outstanding
            // Volatile open session; middle office snapshot ready, analyst queue backed up
            // ------------------------------------------------------------------
            Map<String, Object> inst3BaseVars = new HashMap<>();
            inst3BaseVars.put("tradeId", "TRD-2026-031088");
            inst3BaseVars.put("instrument", "GBP/USD Call Option");
            inst3BaseVars.put("notionalAmount", 50000000.0);
            inst3BaseVars.put("currency", "GBP");
            inst3BaseVars.put("trader", "Marcus Bright");
            inst3BaseVars.put("tradeDesk", "FX Options Desk");

            Map<String, Object> inst3MarketVars = new HashMap<>();
            inst3MarketVars.put("snapshotTimestamp", "2026-03-13T08:30:00Z");
            inst3MarketVars.put("gbpUsdSpot", 1.2731);
            inst3MarketVars.put("impliedVolatility30d", 0.1247);
            inst3MarketVars.put("gbpSonia3m", 0.0512);
            inst3MarketVars.put("marketDataProvider", "Bloomberg Terminal BLP");
            inst3MarketVars.put("sessionVolatilityNote", "Elevated intraday vol: GBP IV +2.1 vols vs prior close. Pre-BoE decision positioning observed across the curve.");
            inst3MarketVars.put("notes", "Market data snapshot filed at 08:30 during a volatile open. Risk analyst assignment still queued — analyst pool at capacity due to quarter-end position reviews across all desks. Metrics SLA: 90 min remaining.");

            ProcessInstance inst3 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk", inst3BaseVars);
            completeTaskWithVars(inst3.getId(), "Task_ProvideMarketData", inst3MarketVars);
            LOG.info("Risk instance 3 (market data filed; analyst metrics pending) – id: {}", inst3.getId());

            // ------------------------------------------------------------------
            // Instance 4: INCIDENT — AWS cloud provisioning failure
            // Spot Fleet capacity exhausted; bid cap exceeded; all 3 retries exhausted
            // On-prem jobs also waiting; cloud path fully blocked
            // ------------------------------------------------------------------
            Map<String, Object> inst4BaseVars = new HashMap<>();
            inst4BaseVars.put("tradeId", "TRD-2026-030947");
            inst4BaseVars.put("instrument", "EUR/GBP Cross-Currency Basis Swap");
            inst4BaseVars.put("notionalAmount", 200000000.0);
            inst4BaseVars.put("currency", "EUR");
            inst4BaseVars.put("trader", "Yuki Tanaka");
            inst4BaseVars.put("tradeDesk", "Rates & FX Hybrid Desk");
            inst4BaseVars.put("cloudProvider", "AWS");
            inst4BaseVars.put("cloudRegion", "eu-west-2");
            inst4BaseVars.put("requestedInstanceType", "r6i.2xlarge");
            inst4BaseVars.put("requestedInstanceCount", 48);
            inst4BaseVars.put("spotBidPriceCap", 2.40);
            inst4BaseVars.put("currentSpotPrice", 4.17);
            inst4BaseVars.put("cloudProvisioningStatus", "FAILED");
            inst4BaseVars.put("cloudErrorCode", "InsufficientInstanceCapacity");
            inst4BaseVars.put("cloudErrorMessage", "AWS CloudFormation stack cf-risk-batch-TRD-2026-030947 FAILED: Spot Fleet request in eu-west-2a has 0 available r6i.2xlarge instances. Current spot price $4.17/hr exceeds configured bid cap $2.40/hr. Stack rollback complete.");
            inst4BaseVars.put("notes", "INCIDENT: Cloud compute path blocked. Spot price spike (triggered by ECB statement at 10:00 UTC) pushed r6i.2xlarge beyond bid cap in eu-west-2. All 3 CloudFormation retries exhausted at 10:23 UTC. On-prem risk jobs are also queued but not yet running. Manual escalation needed: raise spot bid cap, switch to on-demand fleet, or reroute all compute to on-prem cluster.");

            ProcessInstance inst4 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk", inst4BaseVars);
            completeTaskWithVars(inst4.getId(), "Task_SpecifyRiskMetrics", Map.of(
                    "varConfidence", 0.99, "varHorizon", 5, "stressScenario", "ECB_RATE_SHOCK_300BPS",
                    "riskAnalyst", "Sofia Bauer", "metricsSubmittedAt", "2026-03-13T09:50:00Z"
            ));
            completeTaskWithVars(inst4.getId(), "Task_ProvideMarketData", Map.of(
                    "snapshotTimestamp", "2026-03-13T09:58:00Z",
                    "eurGbpSpot", 0.8547, "ois5yRate", 0.0283,
                    "marketDataProvider", "Refinitiv Elektron"
            ));
            // Both user tasks done → parallel fork fires. Do NOT execute cloud job; create incident on it.
            createIncidentAtActivity(inst4.getId(), "Task_ProvisionCloud",
                    "AWS CloudFormation stack cf-risk-batch-TRD-2026-030947 FAILED: " +
                    "ResourceLimitExceeded — Spot Fleet capacity exhausted in eu-west-2a. " +
                    "Requested 48x r6i.2xlarge (spot, bid cap $2.40/hr). " +
                    "Current spot price $4.17/hr. All 3 provisioning retries exhausted at 10:23 UTC. " +
                    "Stack rolled back. On-demand fallback not configured.");
            LOG.info("Risk instance 4 (cloud provisioning incident – spot capacity exhausted) – id: {}", inst4.getId());

            // ------------------------------------------------------------------
            // Instance 5: On-prem jobs done; cloud engine tearing down
            // Parallel paths: on-prem HPC cluster completed; cloud batch finalising
            // ------------------------------------------------------------------
            Map<String, Object> inst5BaseVars = new HashMap<>();
            inst5BaseVars.put("tradeId", "TRD-2026-030812");
            inst5BaseVars.put("instrument", "S&P 500 Variance Swap");
            inst5BaseVars.put("notionalAmount", 10000000.0);
            inst5BaseVars.put("currency", "USD");
            inst5BaseVars.put("trader", "Amira Hassan");
            inst5BaseVars.put("tradeDesk", "Equity Derivatives Desk");
            inst5BaseVars.put("onPremCluster", "risk-hpc-cluster-lon-02");
            inst5BaseVars.put("onPremJobsCompleted", 842193);
            inst5BaseVars.put("onPremComputeTimeSeconds", 742);
            inst5BaseVars.put("cloudEngine", "AWS eu-west-2 — 32x r6i.2xlarge");
            inst5BaseVars.put("cloudJobsCompleted", 505098);
            inst5BaseVars.put("notes", "On-prem HPC cluster (risk-hpc-cluster-lon-02) completed 842,193 Greeks calculations in 742s and is idle. Cloud batch (32x r6i.2xlarge, eu-west-2) completed 505,098 calculations. Cloud engine currently tearing down — results from both paths not yet aggregated.");

            ProcessInstance inst5 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk", inst5BaseVars);
            completeTaskWithVars(inst5.getId(), "Task_SpecifyRiskMetrics", Map.of(
                    "varConfidence", 0.95, "varHorizon", 1, "stressScenario", "VIX_SPIKE_20PCT",
                    "riskAnalyst", "Tom Hargreaves"
            ));
            completeTaskWithVars(inst5.getId(), "Task_ProvideMarketData", Map.of(
                    "snapshotTimestamp", "2026-03-13T07:45:00Z",
                    "sp500Level", 5284.72, "vix", 22.1,
                    "marketDataProvider", "ICE Data Services"
            ));
            executeJobForActivity(inst5.getId(), "Task_OnPremRiskJobs");
            executeJobForActivity(inst5.getId(), "Task_ProvisionCloud");
            executeJobForActivity(inst5.getId(), "Task_RunCloudRiskJobs");
            runtimeService.suspendProcessInstanceById(inst5.getId());
            LOG.info("Risk instance 5 (on-prem complete; cloud tearing down) – id: {}", inst5.getId());

            // ------------------------------------------------------------------
            // Instance 6: Aggregating results — 1.35M calculations from dual paths
            // Both compute paths joined; consolidating Greeks and scenario P&L vectors
            // ------------------------------------------------------------------
            Map<String, Object> inst6BaseVars = new HashMap<>();
            inst6BaseVars.put("tradeId", "TRD-2026-030655");
            inst6BaseVars.put("instrument", "Brent Crude Oil Futures Basket");
            inst6BaseVars.put("notionalAmount", 28000000.0);
            inst6BaseVars.put("currency", "USD");
            inst6BaseVars.put("trader", "Daniel Kowalski");
            inst6BaseVars.put("tradeDesk", "Commodities Desk");
            inst6BaseVars.put("totalRiskCalculations", 1347291);
            inst6BaseVars.put("onPremCalculations", 897440);
            inst6BaseVars.put("cloudCalculations", 449851);
            inst6BaseVars.put("aggregationStartTime", "2026-03-13T10:22:00Z");
            inst6BaseVars.put("notes", "Both compute paths complete: 1,347,291 total risk calculations (897,440 on-prem + 449,851 cloud). Aggregation service is consolidating Greeks, cross-gamma sensitivities, and 500 scenario P&L vectors. Large result set — estimated 3–4 min to consolidate before risk review can begin.");

            ProcessInstance inst6 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk", inst6BaseVars);
            completeTaskWithVars(inst6.getId(), "Task_SpecifyRiskMetrics", Map.of(
                    "varConfidence", 0.99, "varHorizon", 10, "stressScenario", "OIL_SUPPLY_SHOCK_40PCT",
                    "riskAnalyst", "Daniel Kowalski"
            ));
            completeTaskWithVars(inst6.getId(), "Task_ProvideMarketData", Map.of(
                    "snapshotTimestamp", "2026-03-13T08:00:00Z",
                    "brentCrudeSpot", 84.37, "oilCarryRate", 0.0195,
                    "marketDataProvider", "Platts OPIS"
            ));
            executeJobForActivity(inst6.getId(), "Task_OnPremRiskJobs");
            executeJobForActivity(inst6.getId(), "Task_ProvisionCloud");
            executeJobForActivity(inst6.getId(), "Task_RunCloudRiskJobs");
            executeJobForActivity(inst6.getId(), "Task_TearDownCloud");
            runtimeService.suspendProcessInstanceById(inst6.getId());
            LOG.info("Risk instance 6 (aggregating – 1.35M calculations) – id: {}", inst6.getId());

            // ------------------------------------------------------------------
            // Instance 7: Risk review — VaR breach detected; escalation pending
            // Computed VaR $2.85M exceeds desk limit $2.5M; tail losses elevated
            // ------------------------------------------------------------------
            Map<String, Object> inst7BaseVars = new HashMap<>();
            inst7BaseVars.put("tradeId", "TRD-2026-030501");
            inst7BaseVars.put("instrument", "EM Sovereign Bond Portfolio (BRL/MXN/ZAR)");
            inst7BaseVars.put("notionalAmount", 320000000.0);
            inst7BaseVars.put("currency", "USD");
            inst7BaseVars.put("trader", "Elena Papadopoulos");
            inst7BaseVars.put("tradeDesk", "EM Fixed Income Desk");
            inst7BaseVars.put("computedVar10day99pct", 2847000.0);
            inst7BaseVars.put("varDeskLimit", 2500000.0);
            inst7BaseVars.put("varBreachAmount", 347000.0);
            inst7BaseVars.put("greeksDelta", -1.24);
            inst7BaseVars.put("greeksGamma", 0.08);
            inst7BaseVars.put("maxTailLossStress", 8120000.0);
            inst7BaseVars.put("stressScenariosBreached", 3);
            inst7BaseVars.put("notes", "ALERT: 10-day 99% VaR of $2.85M exceeds desk limit of $2.5M by $347k. Three stress scenarios breached threshold (EM contagion, Fed emergency hike +75bps, EM FX crisis 2013-style) with tail losses up to $8.1M. Risk dept review in progress — desk head escalation is likely. Position reduction or cross-desk hedge may be recommended by risk committee.");

            ProcessInstance inst7 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk", inst7BaseVars);
            completeTaskWithVars(inst7.getId(), "Task_SpecifyRiskMetrics", Map.of(
                    "varConfidence", 0.99, "varHorizon", 10, "stressScenario", "EM_CONTAGION_FULL",
                    "riskAnalyst", "Elena Papadopoulos"
            ));
            completeTaskWithVars(inst7.getId(), "Task_ProvideMarketData", Map.of(
                    "snapshotTimestamp", "2026-03-13T07:00:00Z",
                    "brlUsdSpot", 0.1943, "mxnUsdSpot", 0.0489, "zarUsdSpot", 0.0524,
                    "emCdsIndex5y", 285, "marketDataProvider", "JP Morgan Markets"
            ));
            executeJobForActivity(inst7.getId(), "Task_OnPremRiskJobs");
            executeJobForActivity(inst7.getId(), "Task_ProvisionCloud");
            executeJobForActivity(inst7.getId(), "Task_RunCloudRiskJobs");
            executeJobForActivity(inst7.getId(), "Task_TearDownCloud");
            executeJobForActivity(inst7.getId(), "Task_AggregateResults");
            LOG.info("Risk instance 7 (review – VaR breach $347k over limit) – id: {}", inst7.getId());

        } catch (Exception e) {
            LOG.error("Failed to start risk instances: {}", e.getMessage(), e);
        }
    }

    private void completeTaskWithVars(String processInstanceId, String taskDefinitionKey,
                                       Map<String, Object> variables) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .list();
        for (Task task : tasks) {
            taskService.complete(task.getId(), variables);
            LOG.info("Completed task '{}' in instance {}", taskDefinitionKey, processInstanceId);
        }
    }

    private void executeJobForActivity(String processInstanceId, String activityId) {
        List<Job> jobs = managementService.createJobQuery()
                .processInstanceId(processInstanceId)
                .activityId(activityId)
                .list();
        for (Job job : jobs) {
            managementService.executeJob(job.getId());
            LOG.info("Executed job for activity '{}' in instance {}", activityId, processInstanceId);
        }
    }

    private void createIncidentAtActivity(String processInstanceId, String activityId, String message) {
        List<Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .activityId(activityId)
                .list();
        for (Execution execution : executions) {
            runtimeService.createIncident("failedJob", execution.getId(), activityId, message);
            LOG.info("Created incident on activity '{}' in instance {}", activityId, processInstanceId);
        }
    }
}
