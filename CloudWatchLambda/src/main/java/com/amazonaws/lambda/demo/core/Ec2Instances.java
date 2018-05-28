package com.amazonaws.lambda.demo.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ec2Instances {
	
	private SortedMap<String, List<String>> instanceList;
	
	public Ec2Instances() {
		instanceList = new TreeMap<>();
	}
	
	public void addToList(String label, String id) {
		if(instanceList.containsKey(label)) {
			List<String> idList = instanceList.get(label);
			idList.add(id);
		}else {
			List<String> idList = new ArrayList<>();
			idList.add(id);
			instanceList.put(label, idList);
		}
	}
	
	public Map<String, List<String>> getInstances(){
		return instanceList;
	}

	private class Ec2Instace{
		String label,id;

		public Ec2Instace(String label, String id) {
			super();
			this.label = label;
			this.id = id;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Ec2Instace other = (Ec2Instace) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			return true;
		}

		private Ec2Instances getOuterType() {
			return Ec2Instances.this;
		}
	}
}
