package org.graylog.plugins.aggregates.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.graylog.plugins.aggregates.alerts.AggregatesAlertCondition;
import org.graylog.plugins.aggregates.rule.Rule;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

public class AggregatesUtil {
    public final static String ALERT_CONDITION_TYPE = "Aggregates Alert";

	public static int timespanToSeconds(String timespan, Calendar cal){
		Period period = Period.parse(timespan);
		Duration duration = period.toDurationFrom(new DateTime(cal.getTime()));
		return duration.toStandardSeconds().getSeconds();
	}

	public static String getAlertConditionDescription(Rule rule){
		String matchDescriptor = rule.getNumberOfMatches() + " or more";
		if (!rule.isMatchMoreOrEqual()){
			matchDescriptor = "less than " + rule.getNumberOfMatches();
		}
		return "The same value of field '" + rule.getField() + "' occurs " + matchDescriptor + " times in a " + rule.getInterval() + " minute interval";
	}
	
	public String buildSummary(Rule rule, EmailConfiguration emailConfiguration, Map<String, Long> matchedTerms, TimeRange timeRange) throws UnsupportedEncodingException {

        final StringBuilder sb = new StringBuilder();

        sb.append("Matched values for field [ " + rule.getField() + " ]\n");

        for (Map.Entry<String, Long> entry : matchedTerms.entrySet()) {
        	
        	sb.append("\nValue: " + entry.getKey() + "\n");
            sb.append("Occurrences: " + entry.getValue() + "\n");                        

            if (!emailConfiguration.isEnabled()) {
                sb.append("\n");
            } else {
                String streamId = rule.getStreamId();
                String search_uri = "";

                if (streamId != null && streamId != "") {
                    search_uri += "/streams/" + streamId;
                }
                search_uri += "/search?rangetype=absolute&fields=message%2Csource%2C" + rule.getField() + "&from=" + timeRange.getFrom() + "&to=" + timeRange.getTo() + "&q=" + URLEncoder.encode(rule.getQuery() + " AND " + rule.getField() + ":\"" + entry.getKey() + "\"", "UTF-8");
                sb.append("Search: " + emailConfiguration.getWebInterfaceUri() + search_uri + "\n");

            }
        }
        return sb.toString();

    }

    public static Map<String, Object> parametersFromRule(Rule rule){
        String query = rule.getQuery();
        String streamId = rule.getStreamId();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("time", rule.getInterval());
        parameters.put("description", AggregatesUtil.getAlertConditionDescription(rule));

        if (rule.isMatchMoreOrEqual()){
            parameters.put("threshold_type", AggregatesAlertCondition.ThresholdType.MORE_OR_EQUAL.toString());
        } else {
            parameters.put("threshold_type", AggregatesAlertCondition.ThresholdType.LESS.toString());
        }

        parameters.put("threshold", rule.getNumberOfMatches());
        parameters.put("grace", 0);
        parameters.put("type", AggregatesUtil.ALERT_CONDITION_TYPE);
        parameters.put("field", rule.getField());
        parameters.put("number_of_matches", rule.getNumberOfMatches());
        parameters.put("match_more_or_equal", rule.isMatchMoreOrEqual());
        parameters.put("backlog", rule.getBacklog());
        parameters.put("repeat_notifications", rule.shouldRepeatNotifications());
        parameters.put("interval", rule.getInterval());
        parameters.put("query", query);
        parameters.put("rule_name", rule.getName());

        return parameters;
    }

    public static String alertConditionTitleFromRule(Rule rule){
        return "Aggregate rule [" + rule.getName() + "] triggered an alert.";
    }
}
