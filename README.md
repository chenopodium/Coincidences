# Coincidences
Investigation of the coincidence "loophole" in Bell type experiments

The purpose of this simulation is to check if a local realistic model can break the CH inequality (J), as in the Giustina 2015 experiment.
This simulation just focuses on the coincidence "loophole", but not using some "cheap" trick. The goal is to do something similar as is 
done in the experiment.
  
Based on Jan-Åke, the experiment used fixed time windows, so this simulation is doing the same.

In this simplified version, all that is done is:
- generate a stream of detection events for each setting a1, a2, b1, b2 based on the probability determined by the angle (cos(angle))
- then, for different window sizes (fixed windows), we simply count how often we see a coincidence.
So if the window size is 5, and we are counting c12 it would check "trials" 1 -5 for a1 and trials 1-5 for b2. If both have at least 1 detection event
registered, it is counted as an event. (Double counts are discarded, but it also works if they are left in)
- Compute the resulting c11 - c12 - c21 - c22 (=j)
- Based on the probabilities alone, J should be negative (they are chosen such that J should be negative)
- J varies considerably based on the window size

References:
- https://arxiv.org/abs/1507.06231
- https://arxiv.org/pdf/1309.0712.pdf
- https://arxiv.org/abs/1511.03190
- http://boydnlo.ca/rochesterarchive/www2.optics.rochester.edu/workgroups/boyd/archive/Quantum%20Imaging/Assets/papers/2009%20Barbosa%20Phys%20Rev%20A%20-%20Wave%20function%20for%20spontaneous%20parametric.pdf
- http://www.askingwhy.org/blog/first-puzzle-just-a-probability/puzzle-piece-6-disentangling-the-entanglement/
- Forum: https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/bell_quantum_foundations/
- Donald Graft: https://pubpeer.com/publications/E0F8384FC19A6034E86D516D03BB38#fb21323


*Arguments:*
- seed seed: the random seed (a number like 12346). The default is 1234
- trials nr trials: the number of pairs that are generated (default is 100000) (This is plenty... larger values just make it slower)

*Examples:*
java -jar Coincidences.jar  (all default values)

It generates a file streams.csv with the angles and all the counts.
(See example in folder)

![Example Result](https://github.com/chenopodium/Coincidences/blob/master/windows.png)

From Jan-Åke (forum discussion):
"Christensen et al is pulsed, Giustina et al uses a continuous pump.
The timing jitter of the TES detectors was reported by Christensen et al to be 500 ns, so therefore they used 1 us coincidence window. I seem to remember that the jitter in Vienna was less than that.
For the Vienna experiment we did an extended analysis in "Bell-inequality violation with entangled photons, free of the coincidence-time loophole",
 Physical Review A, 90:032107, 2014]. 
 This analysis includes the problem Graft is reporting. Especially, Fig 3 gives the same picture as that picture in Graft's paper. I quote:
"Since the fixed-time-slots method removes coincidences as compared to the moving windows method [compare Figs.2(a)and2(b)], 
it comes at a cost in a continuously pumped experiment: because of the inherently random emission times and the timing jitter of the detectors, 
two photons close in time that would be coincident in the moving-window method may belong to different time slots and fail to register a 
coincidence in the fixed-time-slots method. To minimize the loss, long adjacent slots (much longer than the timing jitter of the detectors used) 
are desirable, and the earlier-mentioned coarse-graining should be used because multiple generated pairs can appear in the same slot.
 Coarse-grained coincidences may not show quantum correlations, and can prohibit or hamper a violation of the tested Bell inequality. Therefore, depending on experimental parameters such as timing jitter, overall efficiency, background counts, and rate of generated pairs, there will be an optimal size for the locally predefined time slots, see Fig.3(b).
For that experiment setup, the optimum is 980 ns, and this should be/is much longer than the timing jitter of the detectors.
We also used what we call the "window-sum" method:
"Finally, we also analyze the data of Ref. [9] using the window-sum method [Figs.2(c) and 3(c)] 
with all three τ_i being equal to τ and the window for A2B2 being 3τ. For τ=180 ns, one obtains J=−96988±2076, a 46σ violation(estimated standard deviations). The window-sum method typically leads to a larger violation than the fixed-time-slots method since it evades the trade-offs encountered in choosing a slot size. The only “penalty” is an increase of the accidental coincidences for the A2B2 events. Therefore, the window-sum method can be a valuable tool in situations where unfavorable experimental parameters (such as high timing jitter and dark counts of the detectors) do not allow a violation using the fixed-time-slots method."
In the curves of Fig 3, the upward slope for the window-sum method only has accidental coincidences for the A2B2 events. 
Those are of course present in the other upward slopes in the figure.