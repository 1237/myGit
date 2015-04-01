/**
 *
 */
package secucompr.file.sender;

/**
 *
 * @author Brightsoft Solutions
 */
public class Huffman {
    
    int redundancy[][];
    
    public void compress(byte[] file)
    {
        redundancy=findItemsRedundancy(file);
        Sort(redundancy);
    }

    public int[][] getRedundancy() {
        return redundancy;
    }

    
    //// Quick Sort Methods /////
    private  void Sort(int[][] redundancy){
	int left = 0;
	int right = redundancy.length-1;     
	quickSort(left, right);
    }
	    	    
    private  void quickSort(int left,int right){
	if(left >= right)
	return;
	         
	int pivot = redundancy[right][1];
	int partition = partition(left, right, pivot);
	       
	quickSort(0, partition-1);
	quickSort(partition+1, right);
	    }
	     
    private int partition(int left,int right,int pivot){
	int leftCursor = left-1;
	int rightCursor = right;
	while(leftCursor < rightCursor){
	while(redundancy[++leftCursor][1] < pivot);
	while(rightCursor > 0 && redundancy[--rightCursor][1] > pivot);
            if(leftCursor >= rightCursor){
            break;
            }else
            {
            swap(leftCursor, rightCursor);
            }
	}
	swap(leftCursor, right);
	return leftCursor;
	}
	     
    private void swap(int first,int second){
	int[] temp = redundancy[first];
	redundancy[first] = redundancy[second];
	redundancy[second] = temp;
	}
    ///// END Quick Sort Methods /////
    
    
    private void buildBinaryTree(int[][] redundancy){
        
    }
    
    public  int[][] findItemsRedundancy(byte[] file) {
        int[] byteRed=new int[256];
        
        for(int i=0;i<file.length;i++)
        {
            byteRed[(int)file[i]]++;
        }
        
        int nonEmptyCount=0;
        for(int i=0;i<256;i++)
        {
            if(byteRed[i]>0)
            {
                nonEmptyCount++;
            }
        }
        int[][] result=new int[nonEmptyCount][2];
        int j=0;
        for(int i=0;i<256;i++)
        {
            if(byteRed[i]>0)
            {
            result[j][0]=i;
            result[j][1]=byteRed[i];
            j++;
            }
        }
        return result;
    }
}
