package org.finos.fluxnova;

import java.util.List;
import java.util.Map;

import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.TaskService;
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
        startLoanInstances();
        startRiskInstances();
    }

    private void startLoanInstances() {
        // Common variables for CheckDocs completion
        Map<String, Object> managerVars = Map.of(
                "creditScore", 400,
                "loanAmount", 100000.0,
                "annualIncome", 50000.0,
                "existingMonthlyDebt", 500.0,
                "employmentStatus", "Employed",
                "loanPurpose", "Personal"
        );
        Map<String, Object> seniorVars = Map.of(
                "creditScore", 350,
                "loanAmount", 600000.0,
                "annualIncome", 200000.0,
                "existingMonthlyDebt", 1000.0,
                "employmentStatus", "Self-Employed",
                "loanPurpose", "Personal"
        );
        Map<String, Object> autoVars = Map.of(
                "creditScore", 700,
                "loanAmount", 100000.0,
                "annualIncome", 80000.0,
                "existingMonthlyDebt", 200.0,
                "employmentStatus", "Employed",
                "loanPurpose", "Car"
        );

        try {
            // Instance 1: at Check Documents (first user task)
            ProcessInstance inst1 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            LOG.info("Loan instance 1 (at Check Documents) – id: {}", inst1.getId());

            // Instance 2: at Employment Eligibility (business rule task)
            ProcessInstance inst2 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst2.getId(), "CheckDocs", managerVars);
            runtimeService.suspendProcessInstanceById(inst2.getId());
            LOG.info("Loan instance 2 (at Employment Eligibility) – id: {}", inst2.getId());

            // Instance 3: at Affordability Check (business rule task)
            ProcessInstance inst3 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst3.getId(), "CheckDocs", managerVars);
            executeJobForActivity(inst3.getId(), "EmploymentCheck");
            runtimeService.suspendProcessInstanceById(inst3.getId());
            LOG.info("Loan instance 3 (at Affordability Check) – id: {}", inst3.getId());

            // Instance 4: at Compliance Review (business rule task)
            ProcessInstance inst4 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst4.getId(), "CheckDocs", managerVars);
            executeJobForActivity(inst4.getId(), "EmploymentCheck");
            executeJobForActivity(inst4.getId(), "AffordabilityCheck");
            runtimeService.suspendProcessInstanceById(inst4.getId());
            LOG.info("Loan instance 4 (at Compliance Review) – id: {}", inst4.getId());

            // Instance 5: at Auto Decision (service task) — credit 700, employed, auto tier
            ProcessInstance inst5 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst5.getId(), "CheckDocs", autoVars);
            executeJobForActivity(inst5.getId(), "EmploymentCheck");
            executeJobForActivity(inst5.getId(), "AffordabilityCheck");
            executeJobForActivity(inst5.getId(), "ComplianceReview");
            runtimeService.suspendProcessInstanceById(inst5.getId());
            LOG.info("Loan instance 5 (at Auto Decision) – id: {}", inst5.getId());

            // Instance 6: at Manager Approval (user task) — credit 400, employed, manager tier
            ProcessInstance inst6 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst6.getId(), "CheckDocs", managerVars);
            executeJobForActivity(inst6.getId(), "EmploymentCheck");
            executeJobForActivity(inst6.getId(), "AffordabilityCheck");
            executeJobForActivity(inst6.getId(), "ComplianceReview");
            LOG.info("Loan instance 6 (at Manager Approval) – id: {}", inst6.getId());

            // Instance 7: at Senior Manager Approval (user task) — credit 350, self-employed, 600k loan
            ProcessInstance inst7 = runtimeService.startProcessInstanceByKey("Process_LoanApproval");
            completeTaskWithVars(inst7.getId(), "CheckDocs", seniorVars);
            executeJobForActivity(inst7.getId(), "EmploymentCheck");
            executeJobForActivity(inst7.getId(), "AffordabilityCheck");
            executeJobForActivity(inst7.getId(), "ComplianceReview");
            LOG.info("Loan instance 7 (at Senior Manager Approval) – id: {}", inst7.getId());
        } catch (Exception e) {
            LOG.error("Failed to start loan instances: {}", e.getMessage(), e);
        }
    }

    private void startRiskInstances() {
        try {
            // Instance 1: at Specify Risk Metrics only
            ProcessInstance inst1 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk");
            completeTask(inst1.getId(), "Task_ProvideMarketData");
            LOG.info("Risk instance 1 (at Specify Risk Metrics) – id: {}", inst1.getId());

            // Instance 2: at Provide Market Data only
            ProcessInstance inst2 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk");
            completeTask(inst2.getId(), "Task_SpecifyRiskMetrics");
            LOG.info("Risk instance 2 (at Provide Market Data) – id: {}", inst2.getId());

            // Instance 3: at On-Prem Risk Jobs + Provision Cloud (parallel fork, both async)
            ProcessInstance inst3 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk");
            completeTask(inst3.getId(), "Task_SpecifyRiskMetrics");
            completeTask(inst3.getId(), "Task_ProvideMarketData");
            // Tokens now waiting at asyncBefore of Task_OnPremRiskJobs and Task_ProvisionCloud
            runtimeService.suspendProcessInstanceById(inst3.getId());
            LOG.info("Risk instance 3 (at On-Prem Risk Jobs + Provision Cloud) – id: {}", inst3.getId());

            // Instance 4: at Run Cloud Risk Jobs (advance cloud path one step)
            ProcessInstance inst4 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk");
            completeTask(inst4.getId(), "Task_SpecifyRiskMetrics");
            completeTask(inst4.getId(), "Task_ProvideMarketData");
            executeJobForActivity(inst4.getId(), "Task_ProvisionCloud");
            // Cloud path now at Task_RunCloudRiskJobs; on-prem path still at Task_OnPremRiskJobs
            runtimeService.suspendProcessInstanceById(inst4.getId());
            LOG.info("Risk instance 4 (at Run Cloud Risk Jobs) – id: {}", inst4.getId());

            // Instance 5: at Tear Down Cloud Engine
            ProcessInstance inst5 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk");
            completeTask(inst5.getId(), "Task_SpecifyRiskMetrics");
            completeTask(inst5.getId(), "Task_ProvideMarketData");
            executeJobForActivity(inst5.getId(), "Task_ProvisionCloud");
            executeJobForActivity(inst5.getId(), "Task_RunCloudRiskJobs");
            // Cloud path now at Task_TearDownCloud; on-prem path still at Task_OnPremRiskJobs
            runtimeService.suspendProcessInstanceById(inst5.getId());
            LOG.info("Risk instance 5 (at Tear Down Cloud Engine) – id: {}", inst5.getId());

            // Instance 6: at Aggregate Risk Results
            ProcessInstance inst6 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk");
            completeTask(inst6.getId(), "Task_SpecifyRiskMetrics");
            completeTask(inst6.getId(), "Task_ProvideMarketData");
            executeJobForActivity(inst6.getId(), "Task_OnPremRiskJobs");
            executeJobForActivity(inst6.getId(), "Task_ProvisionCloud");
            executeJobForActivity(inst6.getId(), "Task_RunCloudRiskJobs");
            executeJobForActivity(inst6.getId(), "Task_TearDownCloud");
            // Both paths complete → join gateway → token at asyncBefore of Task_AggregateResults
            runtimeService.suspendProcessInstanceById(inst6.getId());
            LOG.info("Risk instance 6 (at Aggregate Risk Results) – id: {}", inst6.getId());

            // Instance 7: at Review Results (execute all service tasks)
            ProcessInstance inst7 = runtimeService.startProcessInstanceByKey("Process_SpeculativeRisk");
            completeTask(inst7.getId(), "Task_SpecifyRiskMetrics");
            completeTask(inst7.getId(), "Task_ProvideMarketData");
            executeJobForActivity(inst7.getId(), "Task_OnPremRiskJobs");
            executeJobForActivity(inst7.getId(), "Task_ProvisionCloud");
            executeJobForActivity(inst7.getId(), "Task_RunCloudRiskJobs");
            executeJobForActivity(inst7.getId(), "Task_TearDownCloud");
            executeJobForActivity(inst7.getId(), "Task_AggregateResults");
            LOG.info("Risk instance 7 (at Review Results) – id: {}", inst7.getId());
        } catch (Exception e) {
            LOG.error("Failed to start risk instances: {}", e.getMessage(), e);
        }
    }

    private void completeTask(String processInstanceId, String taskDefinitionKey) {
        completeTaskWithVars(processInstanceId, taskDefinitionKey, Map.of());
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
}
