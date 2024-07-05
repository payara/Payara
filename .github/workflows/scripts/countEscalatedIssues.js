module.exports = async ({github, context, core}) => {
    const { owner, repo } = context.repo;
    const statusAccepted = "Status: Accepted";
    const firstDay = process.env.first_day;
    const lastDay = process.env.last_day;

    // Query all issues updated last month with the accepted label
    const issueQueryResponse = await github.rest.issues.listForRepo({
        owner,
        repo,
        since: firstDay,
        labels: statusAccepted,
        state: 'all',
        per_page: 100
    });

    const acceptedIssues = issueQueryResponse.data;
    let acceptedCount = 0;
    for(const issue of acceptedIssues){
        //For each issue, get its event history
        const events = await github.rest.issues.listEvents({
            owner,
            repo,
            issue_number: issue.number,
            per_page: 100
        });
        let escalatedFilter = event => {
            //Filter which events correspond to the 'labeled' type, for the correct label and during the timeframe for the dates supplied
            const eventDate = new Date(event.created_at).getTime();
            return event.event === 'labeled' && event.label.name === statusAccepted && eventDate >= new Date(firstDay).getTime() && eventDate <= new Date(lastDay).getTime();
        }
        if(events.data.some(escalatedFilter)){
            acceptedCount++;
        }
    }
    console.log(`Total Issues in Accepted status last month: ${acceptedIssues.length}`);
    console.log(`Escalated Issues in the last month: ${acceptedCount}`);
    return acceptedCount;
}
